package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.constant.ErrorConst.DATE_RANGE_FROM_LATER_THAN_TO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_BIRTHDAY_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_FIRST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_NULL_OR_BLANK
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_LAST_NAME_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_START_LATER_THAN_END
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_HISTORY_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_PARTICIPANT_U
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.GroupWithoutMemberReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PartialUserReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.ParticipantReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.hiddenElementsAuthorities
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
import org.springframework.http.HttpStatus.CREATED
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

class ParticipantControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IParticipantService

	@MockitoBean
	private lateinit var readerMapper: ParticipantReaderDtoMapper

	@MockitoBean
	private lateinit var groupReaderMapper: GroupWithoutMemberReaderDtoMapper

	@MockitoBean
	private lateinit var movementReaderMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var partialUserReaderMapper: PartialUserReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ParticipantWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/participants"

		@JvmStatic
		fun `Should findParticipants return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, "text", null, null, null),
			Arguments.of(null, null, null, null, true, null, null),
			Arguments.of(null, null, null, null, null, PresenceStatusEnum.IN, null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findParticipants throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(1_000_001, null, PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Should findParticipantMovements return 200`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(null, null, null, null, null, null),
				Arguments.of(0, null, null, null, null, null),
				Arguments.of(null, 200, null, null, null, null),
				Arguments.of(null, null, null, null, null, null),
				Arguments.of(null, null, true, null, null, null),
				Arguments.of(null, null, null, IN, null, null),
				Arguments.of(null, null, null, null, "2024-11-14T18:34:33.000Z", null),
				Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
			)
		}

		@JvmStatic
		fun `Should findParticipantMovements throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(1_000_001, null, PAGE_NUMBER_IS_UPPER_THAN_MAX_PAGE),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Wrong ParticipantDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				ParticipantWriterDto(lastName = "DOE", birthday = LocalDate.EPOCH),
				PARTICIPANT_FIRST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "", lastName = "DOE", birthday = LocalDate.EPOCH),
				PARTICIPANT_FIRST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(
					firstName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
					lastName = "DOE",
					birthday = LocalDate.EPOCH
				),
				PARTICIPANT_FIRST_NAME_TOO_LONG,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "John", birthday = LocalDate.EPOCH),
				PARTICIPANT_LAST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "John", lastName = "", birthday = LocalDate.EPOCH),
				PARTICIPANT_LAST_NAME_NULL_OR_BLANK,
			),
			Arguments.of(
				ParticipantWriterDto(
					firstName = "John",
					lastName = "azertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiopazertyuiop",
					birthday = LocalDate.EPOCH
				),
				PARTICIPANT_LAST_NAME_TOO_LONG,
			),
			Arguments.of(
				ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.MAX),
				PARTICIPANT_BIRTHDAY_FUTURE,
			),
			Arguments.of(
				ParticipantWriterDto(
					firstName = "John",
					lastName = "DOE",
					birthday = LocalDate.EPOCH,
					startAvailability = CustomDateTimeWriterDto(LocalDate.MAX, OffsetTime.MAX),
					endAvailability = CustomDateTimeWriterDto(LocalDate.MIN, OffsetTime.MIN),
				),
				PARTICIPANT_START_LATER_THAN_END,
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipants return 200`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		query: String?,
		isVisible: Boolean?,
		status: PresenceStatusEnum?,
		at: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber.toLong() * expectedPageSize, expectedPageSize)
		val searchParams = ParticipantSearchParamModel(
			query = query,
			isVisible = isVisible,
			status = status,
			at = at?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findParticipantsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
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
						Pair("status", status),
						Pair("at", at),
					),
				)
			)
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findParticipantsPage(defaultProjectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(expectedPage)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipants throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
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
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).findParticipantById(defaultProjectId, uuid, isVisible = null)
	}

	@Test
	fun `Should findParticipantById force the visible filter when the user cannot read hidden elements`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).findParticipantById(defaultProjectId, uuid, isVisible = true)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipantMovements return 200`(
		page: Int?,
		size: Int?,
		isVisible: Boolean?,
		type: MovementTypeEnum?,
		from: String?,
		to: String?,
	) {
		// Arrange
		val uuid = UUID.randomUUID()
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber.toLong() * expectedPageSize, expectedPageSize)
		val searchParams = MovementSearchParamModel(
			isVisible = isVisible,
			type = type,
			from = from?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			to = to?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findParticipantMovementsPage(any(), any(), any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(movementReaderMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R), *hiddenElementsAuthorities())
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
					listOf(defaultProjectId, uuid),
					listOf(
						Pair("page", page),
						Pair("size", size),
						Pair("isVisible", isVisible),
						Pair("type", type),
						Pair("from", from),
						Pair("to", to),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findParticipantMovementsPage(any(), eq(defaultProjectId), eq(uuid), eq(pageable), eq(searchParams))
		verify(movementReaderMapper).toDtoPage(expectedPage)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(groupReaderMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findParticipantMovements throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_HISTORY_R), *hiddenElementsAuthorities())
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/{id}/movements",
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

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(groupReaderMapper)
	}

	@Test
	fun `Should searchUsers return 200`() {
		// Arrange
		val searched = "John"
		val user = UserModel()
		whenever(service.searchUsersByText(any(), anyOrNull())).thenReturn(Flux.just(user))
		whenever(partialUserReaderMapper.toDto(any())).thenReturn(PartialUserReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(
				uriBuilder("$BASE_URL/search/users", listOf(defaultProjectId), listOf(Pair("q", searched)))
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchUsersByText(defaultProjectId, searched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verify(partialUserReaderMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchGroups return 200`() {
		// Arrange
		val searched = "Group"
		val group = GroupModel()
		whenever(service.searchGroupsByText(any(), anyOrNull())).thenReturn(Flux.just(group))
		whenever(groupReaderMapper.toDto(any())).thenReturn(GroupReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_METADATA_R))
			.get()
			.uri(
				uriBuilder("$BASE_URL/search/groups", listOf(defaultProjectId), listOf(Pair("q", searched)))
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchGroupsByText(defaultProjectId, searched)
		verifyNoInteractions(readerMapper)
		verify(groupReaderMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createParticipant return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)
		whenever(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<ParticipantReaderDto>(CREATED)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, defaultProjectId)
		verify(service).createParticipant(any(), any())
	}

	@Test
	fun `Should createParticipant return 201 with User and Groups`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val participant = ParticipantWriterDto(
			firstName = "John",
			lastName = "DOE",
			birthday = LocalDate.EPOCH,
			userId = UUID.randomUUID(),
			groupIds = listOf(UUID.randomUUID(), UUID.randomUUID())
		)
		whenever(service.createParticipant(any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto().apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<ParticipantReaderDto>(CREATED)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, defaultProjectId)
		verify(service).createParticipant(any(), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong ParticipantDto")
	fun `Should createParticipant return 400`(
		participant: ParticipantWriterDto,
		expectedCode: String,
	) {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}


	@Test
	fun `Should updateParticipant return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)

		whenever(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, defaultProjectId)
		verify(service).updateParticipantById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@Test
	fun `Should updateParticipant accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)

		whenever(service.updateParticipantById(any(), any(), any(), any())).thenReturn(Mono.just(ParticipantModel()))
		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(ParticipantModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(participant)
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verify(writerMapper).toModel(participant, defaultProjectId)
		verify(service).updateParticipantById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@Test
	fun `Should updateParticipantById throw when the element changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val participant = ParticipantWriterDto(firstName = "John", lastName = "DOE", birthday = LocalDate.EPOCH)

		whenever(writerMapper.toModel(any(), any())).thenReturn(ParticipantModel())
		whenever(service.findParticipantById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(ParticipantModel().apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateParticipantById(any(), eq(defaultProjectId), eq(uuid), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong ParticipantDto")
	fun `Should updateParticipantById return 400`(
		participant: ParticipantWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should disableParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.disableParticipantById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).disableParticipantById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should enableParticipantById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.enableParticipantById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(ParticipantModel()))
		whenever(readerMapper.toDto(any())).thenReturn(ParticipantReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_D))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<ParticipantReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).enableParticipantById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should deleteParticipantById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteParticipantById(any(), any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(groupReaderMapper)
		verifyNoInteractions(partialUserReaderMapper)
		verifyNoInteractions(writerMapper)
		verify(service).deleteParticipantById(any(), eq(defaultProjectId), eq(uuid))
	}

	@Test
	fun `Should findParticipants forward the account and birthday filters to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = ParticipantSearchParamModel(
			query = null,
			at = null,
			hasUser = true,
			birthdayFrom = LocalDate.of(2000, 1, 1),
			birthdayTo = LocalDate.of(2010, 12, 31),
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findParticipantsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(Pair("hasUser", true), Pair("birthdayFrom", "2000-01-01"), Pair("birthdayTo", "2010-12-31")),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findParticipantsPage(defaultProjectId, pageable, searchParams)
	}

	@Test
	fun `Should findParticipants throw when the birthday period is inverted`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
					listOf(Pair("birthdayFrom", "2010-12-31"), Pair("birthdayTo", "2000-01-01")),
				)
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, DATE_RANGE_FROM_LATER_THAN_TO)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should createParticipant refuse more than 200 groups`() {
		// Arrange
		val participant = ParticipantWriterDto(
			firstName = "John",
			lastName = "DOE",
			birthday = LocalDate.EPOCH,
			groupIds = List(201) { UUID.randomUUID() })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(participant)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findParticipants forward the on-site filter and the status sort to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = ParticipantSearchParamModel(
			query = null,
			at = null,
			isOnSite = true,
			sort = SortModel("status", false),
		)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findParticipantsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("onSite", true), Pair("sort", "status"))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findParticipantsPage(defaultProjectId, pageable, searchParams)
	}

	@Test
	fun `Should findParticipants forward the opposite on-site filter to the service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = ParticipantSearchParamModel(query = null, at = null, isOnSite = false)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(ParticipantModel()))
		whenever(service.findParticipantsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(ParticipantReaderDto())),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_PARTICIPANT_R), *hiddenElementsAuthorities())
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("onSite", false))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findParticipantsPage(defaultProjectId, pageable, searchParams)
	}
}
