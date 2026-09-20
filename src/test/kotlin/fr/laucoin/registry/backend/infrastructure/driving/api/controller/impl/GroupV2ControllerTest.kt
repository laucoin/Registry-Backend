package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.GroupError.GROUP_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_GROUP_U
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.service.IGroupService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GroupWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.AddedGroupMembersReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.GroupWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class GroupV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IGroupService

	@MockitoBean
	private lateinit var readerMapper: GroupReaderDtoMapper

	@MockitoBean
	private lateinit var readerLightMapper: GroupWithoutMemberReaderDtoMapper

	@MockitoBean
	private lateinit var participantReaderMapper: ParticipantReaderDtoMapper

	@MockitoBean
	private lateinit var addedGroupMembersReaderMapper: AddedGroupMembersReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: GroupWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/groups"

		@JvmStatic
		fun `Should findGroupsArrivingToday and findGroupsDepartingToday return 400 on an invalid limit`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(51, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}
	}

	@Test
	fun `Should findGroups call service with page, size and empty sort by default`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(GroupModel()))
		whenever(service.findGroupsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findGroupsPage(
			projectId,
			pageable,
			GroupSearchParamModel(textSearched = null, visibilitySearched = null, presenceSearched = null, dateTimeSearched = null),
			emptyList(),
		)
		verify(readerLightMapper, atLeastOnce()).toDto(any())
	}

	@Test
	fun `Should findGroups return 400 on an unknown sort field`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "unknownField"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findGroups return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findGroupsArrivingToday return 200`() {
		// Arrange
		whenever(service.findArrivingToday(any(), any())).thenReturn(Flux.just(GroupModel()))
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/arrivals-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findArrivingToday(projectId, 5)
	}

	@Test
	fun `Should findGroupsDepartingToday return 200`() {
		// Arrange
		whenever(service.findDepartingToday(any(), any())).thenReturn(Flux.just(GroupModel()))
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/departures-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findDepartingToday(projectId, 5)
	}

	@Test
	fun `Should findGroupsArrivingToday return 403 without REGISTRY_PROJECT_GROUP_R`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/arrivals-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findGroupsDepartingToday return 403 without REGISTRY_PROJECT_GROUP_R`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/departures-today", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findGroupsArrivingToday and findGroupsDepartingToday return 400 on an invalid limit`(
		limit: Int,
		expectedCode: String,
	) {
		// Act
		val arriving = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/arrivals-today", listOf(projectId), listOf(Pair("limit", limit))))
			.exchange()
		val departing = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/departures-today", listOf(projectId), listOf(Pair("limit", limit))))
			.exchange()

		// Assert
		arriving.assertError(BAD_REQUEST, expectedCode)
		departing.assertError(BAD_REQUEST, expectedCode)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findGroupMembersByGroupId drop the Searched suffix and call service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val page = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findGroupMembersPageByGroupId(any(), any(), any(), any())).thenReturn(Mono.just(page))
		whenever(participantReaderMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)
		verify(service).findGroupMembersPageByGroupId(eq(projectId), eq(uuid), eq(pageable), any())
	}

	@Test
	fun `Should findGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findGroupById(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Mono.just(GroupModel()))
		whenever(readerMapper.toDto(any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<GroupReaderDto>(OK)
		verify(service).findGroupById(
			projectId,
			uuid,
			visibilitySearched = null,
			memberVisibilitySearched = null,
			memberAvailabilitySearched = null,
		)
	}

	@Test
	fun `Should searchParticipants rename textSearched to q`() {
		// Arrange
		val searched = "John"
		whenever(service.searchParticipantsByText(any(), anyOrNull())).thenReturn(Flux.just(ParticipantModel()))
		whenever(participantReaderMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/search/participants", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchParticipantsByText(projectId, searched)
	}

	@Test
	fun `Should createGroup return 201 with a Location header`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val createdId = UUID.randomUUID()
		val group = GroupWriterDto(name = "name", members = listOf(uuid))
		whenever(service.createGroup(any(), any())).thenReturn(Mono.just(GroupModel().apply { id = createdId }))
		whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(GroupWithoutMemberReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/groups/$createdId"))

		verify(service).createGroup(any(), any())
	}

	@Test
	fun `Should createGroup return 400`() {
		// Arrange
		val group = GroupWriterDto(name = null, members = listOf(UUID.randomUUID()))

		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, GROUP_NAME_NULL_OR_BLANK)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val group = GroupWriterDto(name = "name", members = listOf(uuid))
		whenever(service.updateGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(GroupModel())
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(group)
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).updateGroupById(any(), eq(projectId), eq(uuid), any())
	}

	@Test
	fun `Should addMembersToGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val memberIds = listOf(UUID.randomUUID())
		whenever(service.addMembersToGroupById(any(), any(), any(), any())).thenReturn(Mono.just(Pair(emptyList(), emptyList())))
		whenever(addedGroupMembersReaderMapper.toDto(any())).thenReturn(AddedGroupMembersReaderDto(emptyList(), emptyList()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/members", listOf(projectId, uuid), emptyList()))
			.bodyValue(memberIds)
			.exchange()

		// Assert
		result.body<AddedGroupMembersReaderDto>(OK)
		verify(service).addMembersToGroupById(any(), eq(projectId), eq(uuid), eq(memberIds))
	}

	@Test
	fun `Should removeMemberFromGroupById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val memberId = UUID.randomUUID()
		whenever(service.removeMemberFromGroupById(any(), any(), any(), any())).thenReturn(Mono.just(GroupModel()))
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}/members/{memberId}", listOf(projectId, uuid, memberId), emptyList()))
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).removeMemberFromGroupById(any(), eq(projectId), eq(uuid), eq(memberId))
	}

	@Test
	fun `Should disableGroupById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.disableGroupById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(GroupModel()))
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).disableGroupById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should enableGroupById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.enableGroupById(any(), eq(projectId), eq(uuid))).thenReturn(Mono.just(GroupModel()))
		whenever(readerLightMapper.toDto(any())).thenReturn(GroupWithoutMemberReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_GROUP_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<GroupWithoutMemberReaderDto>(OK)
		verify(service).enableGroupById(any(), eq(projectId), eq(uuid))
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)
		verify(service).deleteGroupById(projectId, uuid)
	}
}
