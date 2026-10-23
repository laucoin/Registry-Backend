package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.test.ModelExt.defaultUserId
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_MEMBERS_TOO_MANY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GroupWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONTENT_TOO_LARGE
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.MULTI_STATUS
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream

class GroupControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IGroupService

	@MockitoBean
	private lateinit var readerMapper: GroupReaderDtoMapper

	@MockitoBean
	private lateinit var lightReaderMapper: GroupWithoutMemberReaderDtoMapper

	@MockitoBean
	private lateinit var participantReaderMapper: ParticipantReaderDtoMapper

	@MockitoBean
	private lateinit var addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: GroupWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val OVER_BODY_LIMIT = 600_000
		private const val UNDER_BODY_LIMIT = 500_000

		private const val BASE_URL = "/api/v2/projects/{projectId}/groups"

		@JvmStatic
		fun `Should findGroups return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, "text", null, null, null),
			Arguments.of(null, null, null, null, true, null, null),
			Arguments.of(null, null, null, null, null, true, null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findGroups throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should findGroupMembersByGroupId return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, "text", null, null, null),
			Arguments.of(null, null, null, null, true, null, null),
			Arguments.of(null, null, null, null, null, IN, null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findGroupMembersByGroupId throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Wrong GroupDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				GroupWriterDto(name = null, members = listOf(UUID.randomUUID())),
				GROUP_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				GroupWriterDto().apply {
					name =
						"azertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazertyazerty"
					members = listOf(UUID.randomUUID())
				},
				GROUP_NAME_TOO_LONG,
			),
			Arguments.of(
				GroupWriterDto(
					name = "name",
					startAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
					members = listOf(UUID.randomUUID())
				),
				GROUP_START_LATER_THAN_END,
			),
			Arguments.of(
				GroupWriterDto().apply { name = "name"; members = null },
				GROUP_MEMBERS_EMPTY,
			),
			Arguments.of(
				GroupWriterDto().apply { name = "name"; members = emptyList() },
				GROUP_MEMBERS_EMPTY,
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findGroups return 200`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		isPresent: Boolean?,
		at: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = GroupSearchParamModel(
			query = query,
			isVisible = isVisible,
			isPresent = isPresent,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(GroupModel()))
		whenever(service.findGroupsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(lightReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(GroupReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("isPresent", isPresent),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findGroupsPage(defaultProjectId, pageable, searchParams)
		verify(lightReaderMapper).toDtoPage(any())
		verifyNoInteractions(participantReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findGroups throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(service)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(participantReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findGroupMembersByGroupId return 200`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		status: PresenceStatusEnum?,
		at: String?,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = ParticipantSearchParamModel(
			query = query,
			isVisible = isVisible,
			status = status,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findGroupMembersPageByGroupId(any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(participantReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/members",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("q", query),
						Pair("isVisible", isVisible),
						Pair("status", status),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findGroupMembersPageByGroupId(defaultProjectId, uuid, pageable, searchParams)
		verifyNoInteractions(readerMapper)
		verify(participantReaderMapper).toDtoPage(any())
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findGroupMembersByGroupId throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/members",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
					),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedMessage)

		verifyNoInteractions(service)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(participantReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findGroupById(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
			Mono.just(
				GroupModel()
			)
		)
		whenever(readerMapper.toDto(any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)

		verify(service).findGroupById(
			defaultProjectId,
			uuid,
			isVisible = null,
			memberIsVisible = null,
			memberIsAvailable = null
		)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchParticipants return 200`() {
		// Arrange
		val searched = "John"
		val participant = ParticipantModel()
		whenever(service.searchParticipantsByText(any(), anyOrNull())).thenReturn(Flux.just(participant))
		whenever(participantReaderMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_METADATA_R))
			.get()
			.uri(
				uriBuilder("${BASE_URL}/search/participants", listOf(defaultProjectId), listOf(Pair("q", searched)))
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchParticipantsByText(defaultProjectId, searched)
		verifyNoInteractions(readerMapper)
		verify(participantReaderMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createGroup return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val uuid = UUID.randomUUID()
		val group = GroupWriterDto(name = "name", members = listOf(uuid))
		whenever(service.createGroup(any(), any())).thenReturn(Mono.just(GroupModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
		whenever(lightReaderMapper.toDto(any())).thenReturn(GroupReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<GroupReaderDto>(CREATED)

		verify(service).createGroup(any(), any())
		verify(lightReaderMapper).toDto(any())
		verifyNoInteractions(participantReaderMapper)
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@ParameterizedTest
	@MethodSource("Wrong GroupDto")
	fun `Should createGroup return 400`(
		group: GroupWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(participantReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val group = GroupWriterDto(name = "name", members = listOf(uuid))

		whenever(service.updateGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
		whenever(lightReaderMapper.toDto(any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)

		verify(lightReaderMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
		verifyNoInteractions(participantReaderMapper)
		verify(service).updateGroupById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@Test
	fun `Should updateGroupById accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val group = GroupWriterDto(name = "name", members = listOf(uuid))

		whenever(service.updateGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
		whenever(lightReaderMapper.toDto(any())).thenReturn(GroupReaderDto())
		whenever(service.findGroupSummaryById(any(), any())).thenReturn(
			Mono.just(GroupModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(group)
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)

		verify(lightReaderMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
		verifyNoInteractions(participantReaderMapper)
		verify(service).updateGroupById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@Test
	fun `Should updateGroupById throw when the element changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val group = GroupWriterDto(name = "name", members = listOf(uuid))

		whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
		whenever(service.findGroupSummaryById(any(), any())).thenReturn(
			Mono.just(GroupModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateGroupById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong GroupDto")
	fun `Should updateGroupById return 400`(
		group: GroupWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(participantReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should addMembersToGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val memberIds = listOf(uuid1, uuid2)

		whenever(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(
			Mono.just(
				Pair(
					memberIds,
					emptyList()
				)
			)
		)
		whenever(addedGroupMembersReaderMapper.toDto(any())).thenReturn(
			AddedGroupMembersReaderDto(
				emptyList(),
				emptyList()
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(memberIds)
			.exchange()

		// Assert
		result.body<AddedGroupMembersReaderDto>(OK)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verify(service).addMembersToGroupById(any(), eq(defaultProjectId), eq(uuid), eq(memberIds))
	}

	@Test
	fun `Should addMembersToGroupById return 207`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val uuid1 = UUID.randomUUID()
		val uuid2 = UUID.randomUUID()
		val memberIds = listOf(uuid1, uuid2)

		whenever(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(
			Mono.just(
				Pair(
					listOf(uuid1),
					listOf(uuid2)
				)
			)
		)
		whenever(addedGroupMembersReaderMapper.toDto(any())).thenReturn(
			AddedGroupMembersReaderDto(
				listOf(uuid1),
				listOf(uuid2)
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(memberIds)
			.exchange()

		// Assert
		result.body<AddedGroupMembersReaderDto>(MULTI_STATUS)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verify(service).addMembersToGroupById(any(), eq(defaultProjectId), eq(uuid), eq(memberIds))
	}

	@Test
	fun `Should removeMemberFromGroupById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val uuid1 = UUID.randomUUID()

		whenever(service.removeMemberFromGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/members/{memberId}", listOf(defaultProjectId, uuid, uuid1), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verifyNoInteractions(lightReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).removeMemberFromGroupById(any(), eq(defaultProjectId), eq(uuid), eq(uuid1))
	}

	@Test
	fun `Should disableGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.disableGroupById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(Mono.just(GroupModel()))
		whenever(lightReaderMapper.toDto(any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)

		verify(lightReaderMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verify(service).disableGroupById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should enableGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.enableGroupById(any(), eq(defaultProjectId), eq(uuid))).thenReturn(Mono.just(GroupModel()))
		whenever(lightReaderMapper.toDto(any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)

		verify(lightReaderMapper).toDto(any())
		verifyNoInteractions(writerMapper)
		verify(service).enableGroupById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should deleteGroupById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteGroupById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(participantReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteGroupById(eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should addMembersToGroupById refuse more than 200 members at once`() {
		// Arrange
		val memberIds = List(201) { UUID.randomUUID() }

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(defaultProjectId, UUID.randomUUID()), emptyList()))
			.bodyValue(memberIds)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, GROUP_MEMBERS_TOO_MANY)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should addMembersToGroupById accept exactly 200 members at once`() {
		// Arrange
		val memberIds = List(200) { UUID.randomUUID() }
		whenever(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(
			Mono.just(
				Pair(
					memberIds,
					emptyList()
				)
			)
		)
		whenever(addedGroupMembersReaderMapper.toDto(any())).thenReturn(
			AddedGroupMembersReaderDto(
				emptyList(),
				emptyList()
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(defaultProjectId, UUID.randomUUID()), emptyList()))
			.bodyValue(memberIds)
			.exchange()

		// Assert
		result.body<AddedGroupMembersReaderDto>(OK)
	}

	@Test
	fun `Should createGroup refuse more than 200 members`() {
		// Arrange
		val group = GroupWriterDto(name = "name", members = List(201) { UUID.randomUUID() })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createGroup refuse a request body above the in-memory limit`() {
		// Arrange
		val group = GroupWriterDto(name = "a".repeat(OVER_BODY_LIMIT), members = emptyList())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.expectStatus().isEqualTo(CONTENT_TOO_LARGE)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createGroup read a request body just under the in-memory limit`() {
		// Arrange
		val group = GroupWriterDto(name = "a".repeat(UNDER_BODY_LIMIT), members = listOf(UUID.randomUUID()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, GROUP_NAME_TOO_LONG)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findGroupMembersByGroupId forward the on-site filter and the status sort to the service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val searchParams = ParticipantSearchParamModel(
			query = null,
			at = null,
			isOnSite = true,
			sort = SortModel("status", true),
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findGroupMembersPageByGroupId(any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(participantReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/members",
					listOf(defaultProjectId, uuid),
					listOf(Pair("onSite", true), Pair("sort", "-status")),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findGroupMembersPageByGroupId(defaultProjectId, uuid, pageable, searchParams)
	}

	@Test
	fun `Should findGroups restrict the search to the groups of the signed-in user when asked for mine`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = GroupSearchParamModel(memberUserId = defaultUserId, hasOnSiteMembers = true)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(GroupModel()))
		whenever(service.findGroupsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(lightReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(GroupWithoutMemberReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("mine", true), Pair("hasOnSiteMembers", true))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findGroupsPage(defaultProjectId, pageable, searchParams)
	}

	@Test
	fun `Should findGroups ignore the signed-in user when not asked for mine`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = GroupSearchParamModel(hasOnSiteMembers = false)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(GroupModel()))
		whenever(service.findGroupsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(lightReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(GroupWithoutMemberReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("mine", false), Pair("hasOnSiteMembers", false))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findGroupsPage(defaultProjectId, pageable, searchParams)
	}

	@Test
	fun `Should findGroups expose the on-site members counters of every group`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(GroupModel()))
		val dto = GroupWithoutMemberReaderDto(
			onSiteMembersCount = 6,
			inMembersCount = 3,
			outMembersCount = 2,
			overdueMembersCount = 1,
		)
		whenever(service.findGroupsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(lightReaderMapper.toDtoPage(any())).thenReturn(PageModel(pageable, totalElements = 1, listOf(dto)))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.expectStatus().isOk
			.expectBody()
			.jsonPath("$.content[0].onSiteMembersCount").isEqualTo(6)
			.jsonPath("$.content[0].inMembersCount").isEqualTo(3)
			.jsonPath("$.content[0].outMembersCount").isEqualTo(2)
			.jsonPath("$.content[0].overdueMembersCount").isEqualTo(1)
	}
}
