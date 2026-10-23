package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.LIST_TOO_LARGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_AND_REASON_ARE_DEFINED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_PARTICIPANT_ID_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_IDS_TOO_MANY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_TOO_LONG
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RESOURCE_MODIFIED_SINCE_READ
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.OTHER
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum.REASON
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.MovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.PlannedMovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantMovementWriterDtoMapper
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
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.PRECONDITION_FAILED
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream

class MovementControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IMovementService

	@MockitoBean
	private lateinit var readerMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var plannedReaderMapper: PlannedMovementReaderDtoMapper

	@MockitoBean
	private lateinit var readerContentMapper: MovementContentReaderDtoMapper

	@MockitoBean
	private lateinit var movementTypeReaderMapper: MovementTypeReaderDtoMapper

	@MockitoBean
	private lateinit var movementParticipantsAndGroupsReaderMapper: MovementParticipantsAndGroupsReaderDtoMapper

	@MockitoBean
	private lateinit var vehicleReaderMapper: VehicleReaderDtoMapper

	@MockitoBean
	private lateinit var activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper

	@MockitoBean
	private lateinit var reasonReaderMapper: MovementReasonReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ParticipantMovementWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/movements"

		@JvmStatic
		fun `Planned endpoints`(): Stream<Arguments> = Stream.of(
			Arguments.of("arrivals", IN),
			Arguments.of("departures", OUT),
		)

		@JvmStatic
		fun `Planned endpoints with a wrong day`(): Stream<Arguments> = Stream.of(
			Arguments.of("arrivals", "not-a-date"),
			Arguments.of("arrivals", "2026-13-40"),
			Arguments.of("departures", "2026-07-14T10:00:00Z"),
			Arguments.of("departures", "14/07/2026"),
		)

		@JvmStatic
		fun `Should findMovements return 200`(): Stream<Arguments> = Stream.of(
			Arguments.of("not locale", null, null, null, null, null, null),
			Arguments.of(null, 0, null, null, null, null, null),
			Arguments.of(null, null, 200, null, null, null, null),
			Arguments.of(null, null, null, null, null, null, null),
			Arguments.of(null, null, null, true, null, null, null),
			Arguments.of(null, null, null, null, IN, null, null),
			Arguments.of(null, null, null, null, null, "2024-11-14T18:34:33.000Z", null),
			Arguments.of(null, null, null, null, null, null, "2024-11-14T18:34:33.000Z"),
		)

		@JvmStatic
		fun `Should findMovements throw due to wrong params`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(-1, null, PAGE_NUMBER_IS_LOWER_THAN_ZERO),
				Arguments.of(null, 0, PAGE_SIZE_IS_LOWER_THAN_ONE),
				Arguments.of(null, 201, PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE),
			)
		}

		@JvmStatic
		fun `Wrong MovementDto`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					activityId = null,
					content = listOf(ParticipantMovementContentWriterDto(participantId = UUID.randomUUID()))
				),
				MOVEMENT_DATETIME_NULL,
			),
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = now(),
					type = null,
					reason = null,
					activityId = null,
					content = listOf(ParticipantMovementContentWriterDto(participantId = UUID.randomUUID()))
				),
				MOVEMENT_TYPE_NULL,
			),
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = now(),
					type = IN,
					reason = OTHER,
					activityId = null,
					content = listOf(ParticipantMovementContentWriterDto(participantId = UUID.randomUUID()))
				),
				MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE,
			),
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = now(),
					type = OUT,
					reason = OTHER,
					activityId = UUID.randomUUID(),
					content = listOf(ParticipantMovementContentWriterDto(participantId = UUID.randomUUID()))
				),
				MOVEMENT_ACTIVITY_AND_REASON_ARE_DEFINED,
			),
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = now(),
					type = IN,
					reason = null,
					activityId = null,
					content = null
				),
				MOVEMENT_CONTENT_EMPTY,
			),
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = now(),
					type = IN,
					reason = null,
					activityId = null,
					content = emptyList()
				),
				MOVEMENT_CONTENT_EMPTY,
			),
			Arguments.of(
				ParticipantMovementWriterDto(
					dateTime = now(),
					type = IN,
					reason = null,
					activityId = null,
					content = listOf(ParticipantMovementContentWriterDto(participantId = null))
				),
				MOVEMENT_CONTENT_PARTICIPANT_ID_NULL,
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findMovements return 200`(
		requestedLocale: String?,
		page: Int?,
		size: Int?,
		isVisible: Boolean?,
		type: MovementTypeEnum?,
		from: String?,
		to: String?,
	) {
		// Arrange
		val expectedPageNumber = page ?: 0
		val expectedPageSize = size ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
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
		whenever(service.findMovementsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(defaultProjectId),
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
			.headers { headers -> requestedLocale?.let { headers.add(ACCEPT_LANGUAGE, it) } }
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findMovementsPage(defaultProjectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findMovements throw due to wrong params`(
		page: Int?,
		size: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
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

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findMovementsContents return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findMovementsContent(any(), any())).thenReturn(
			Flux.just(
				Pair(
					uuid,
					listOf(MovementModel.MovementContentModel())
				)
			)
		)
		whenever(readerContentMapper.toDto(any())).thenReturn(MovementContentReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/contents", listOf(defaultProjectId), listOf(Pair("movementIds", uuid))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).findMovementsContent(defaultProjectId, listOf(uuid))
		verifyNoInteractions(readerMapper)
		verify(readerContentMapper).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findMovementsContents throw due to too many movement ids`() {
		// Arrange
		val movementIds = List(81) { Pair("movementIds", UUID.randomUUID()) }

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/contents", listOf(defaultProjectId), movementIds))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, MOVEMENT_IDS_TOO_MANY)

		verifyNoInteractions(service)
	}

	@Test
	fun `Should findMovementById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(
			service.findMovementById(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).findMovementById(defaultProjectId, uuid, isVisible = null)
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchReasonsAndActivities return 200`() {
		// Arrange
		val searched = "text"
		val type = IN
		val contentType = REGISTERED
		whenever(service.searchActivitiesByText(any(), any(), anyOrNull())).thenReturn(Flux.just(ActivityModel()))
		whenever(service.searchReasonsByText(any(), any())).thenReturn(Flux.just(OTHER))
		whenever(activityReasonReaderMapper.toDto(any())).thenReturn(
			MovementReasonsReaderDto(
				value = "value",
				label = "label",
				kind = REASON,
				type = IN,
			)
		)
		whenever(reasonReaderMapper.toDto(any())).thenReturn(
			MovementReasonsReaderDto(
				value = "value",
				label = "label",
				kind = REASON,
				type = IN,
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/search/reasons",
					listOf(defaultProjectId),
					listOf(
						Pair("type", type),
						Pair("contentType", contentType),
						Pair("q", searched)
					)
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchActivitiesByText(defaultProjectId, contentType, searched)
		verify(service).searchReasonsByText(contentType, type)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(vehicleReaderMapper)
		verify(activityReasonReaderMapper).toDto(any())
		verify(reasonReaderMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchParticipantsAndGroups return 200`() {
		// Arrange
		val searched = "text"
		val type = REGISTERED
		whenever(service.searchParticipantsAndGroupsByText(any(), any(), anyOrNull())).thenReturn(
			Mono.just(
				Tuples.of(
					listOf(ParticipantModel()),
					listOf(GroupModel()),
				)
			)
		)
		whenever(movementParticipantsAndGroupsReaderMapper.toDto(any())).thenReturn(
			MovementParticipantsAndGroupsReaderDto(
				emptyList(),
				emptyList(),
			)
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/search/participants-and-groups",
					listOf(defaultProjectId),
					listOf(Pair("contentType", type), Pair("q", searched))
				)
			)
			.exchange()

		// Assert
		result.body<MovementParticipantsAndGroupsReaderDto>(OK)

		verify(service).searchParticipantsAndGroupsByText(defaultProjectId, type, searched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verify(movementParticipantsAndGroupsReaderMapper).toDto(any())
		verifyNoInteractions(vehicleReaderMapper)
		verifyNoInteractions(activityReasonReaderMapper)
		verifyNoInteractions(reasonReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchVehicles return 200`() {
		// Arrange
		val searched = "text"
		whenever(service.searchVehiclesByText(any(), anyOrNull())).thenReturn(Flux.just(VehicleModel()))
		whenever(vehicleReaderMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/search/vehicles", listOf(defaultProjectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchVehiclesByText(defaultProjectId, searched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(vehicleReaderMapper).toDto(any())
		verifyNoInteractions(activityReasonReaderMapper)
		verifyNoInteractions(reasonReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createMovement return 201`() {
		// Arrange
		val createdId = UUID.randomUUID()
		val uuid = UUID.randomUUID()
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(ParticipantMovementContentWriterDto(participantId = uuid))
		)
		whenever(
			service.createMovement(
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED).apply {
			id = createdId
		})

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.expectHeader()
			.valueEquals("Location", "${BASE_URL.replace("{projectId}", defaultProjectId.toString())}/$createdId")
		result.body<MovementReaderDto>(CREATED)

		verify(service).createMovement(any(), any(), any())
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
	}

	@ParameterizedTest
	@MethodSource("Wrong MovementDto")
	fun `Should createMovement return 400`(
		movement: ParticipantMovementWriterDto,
		expectedCode: String,
	) {
		// Arrange
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should updateMovementById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(ParticipantMovementContentWriterDto(participantId = uuid))
		)

		whenever(
			service.updateMovementById(
				any(),
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
		verify(service).updateMovementById(any(), eq(defaultProjectId), eq(uuid), any(), any())
	}

	@Test
	fun `Should updateMovementById accept the matching If-Match`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(ParticipantMovementContentWriterDto(participantId = uuid))
		)

		whenever(
			service.updateMovementById(
				any(),
				any(),
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))
		whenever(service.findMovementById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(MovementModel(contentType = REGISTERED).apply {
				lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
			}),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(movement)
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(writerMapper).toModel(any(), eq(defaultProjectId))
		verify(service).updateMovementById(any(), eq(defaultProjectId), eq(uuid), any(), any())
	}

	@Test
	fun `Should updateMovementById throw when the movement changed since it was read`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(ParticipantMovementContentWriterDto(participantId = uuid))
		)
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(service.findMovementById(any(), any(), anyOrNull())).thenReturn(
			Mono.just(
				MovementModel(contentType = REGISTERED).apply {
					lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
				}
			),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(movement)
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateMovementById(any(), any(), any(), any(), any())
	}

	@ParameterizedTest
	@MethodSource("Wrong MovementDto")
	fun `Should updateMovementById return 400`(
		movement: ParticipantMovementWriterDto,
		expectedCode: String,
	) {
		// Arrange
		val uuid = UUID.randomUUID()

		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, expectedCode)

		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should disableMovementById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.disableMovementById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).disableMovementById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should enableMovementById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(
			service.enableMovementById(
				any(),
				eq(defaultProjectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))


		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).enableMovementById(any(), eq(defaultProjectId), eq(uuid))
		verify(readerMapper).toDto(any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteMovementById return 204`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteMovementById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(defaultProjectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).deleteMovementById(defaultProjectId, uuid)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findMovements forward the activity filter to the service`() {
		// Arrange
		val activityId = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val expectedPage = PageModel(pageable, totalElements = 1, listOf(MovementModel()))
		whenever(service.findMovementsPage(any(), any(), any())).thenReturn(Mono.just(expectedPage))
		whenever(readerMapper.toDtoPage(any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R), buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("activityId", activityId))))
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findMovementsPage(
			defaultProjectId,
			pageable,
			MovementSearchParamModel(type = null, activityId = activityId)
		)
	}

	@Test
	fun `Should findMovements refuse the activity filter without the activity option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), listOf(Pair("activityId", UUID.randomUUID()))))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}

	@ParameterizedTest
	@MethodSource("Planned endpoints")
	fun `Should findPlanned return 200 and default the day`(path: String, type: MovementTypeEnum) {
		// Arrange
		whenever(
			service.findPlannedMovements(
				any(),
				any(),
				anyOrNull()
			)
		).thenReturn(Flux.just(PlannedMovementModel(IN, null, listOf(ParticipantModel()))))
		whenever(plannedReaderMapper.toDto(any())).thenReturn(PlannedMovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.get()
			.uri(uriBuilder("$BASE_URL/planned/$path", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).findPlannedMovements(defaultProjectId, type, null)
		verify(plannedReaderMapper).toDto(any())
	}

	@ParameterizedTest
	@MethodSource("Planned endpoints")
	fun `Should findPlanned forward the requested day`(path: String, type: MovementTypeEnum) {
		// Arrange
		val date = LocalDate.of(2026, 7, 14)
		whenever(service.findPlannedMovements(any(), any(), anyOrNull())).thenReturn(Flux.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.get()
			.uri(uriBuilder("$BASE_URL/planned/$path", listOf(defaultProjectId), listOf(Pair("date", date))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).findPlannedMovements(defaultProjectId, type, date)
	}

	@ParameterizedTest
	@MethodSource("Planned endpoints with a wrong day")
	fun `Should findPlanned return 400 with a wrong day`(path: String, date: String) {
		// Arrange
		val authority = buildAuthority(REGISTRY_PROJECT_MOVEMENT_C)

		// Act
		val result = webClient
			.authenticate(authority)
			.get()
			.uri(uriBuilder("$BASE_URL/planned/$path", listOf(defaultProjectId), listOf(Pair("date", date))))
			.exchange()

		// Assert
		result.expectStatus().isBadRequest

		verifyNoInteractions(service)
	}

	@ParameterizedTest
	@MethodSource("Planned endpoints")
	fun `Should findPlanned return 403 without the movement creation permission`(path: String) {
		// Arrange
		val authority = buildAuthority(REGISTRY_PROJECT_MOVEMENT_R)

		// Act
		val result = webClient
			.authenticate(authority)
			.get()
			.uri(uriBuilder("$BASE_URL/planned/$path", listOf(defaultProjectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)

		verifyNoInteractions(service)
	}

	private fun guestMovement() = GuestMovementWriterDto(
		dateTime = now(),
		type = OUT,
		reason = null,
		content = listOf(MovementContentWriterDto(participantId = UUID.randomUUID())),
		guests = null,
	)

	private fun guestModel() = MovementModel(contentType = GUEST).apply {
		lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11Z"))
	}

	@Test
	fun `Should createGuestsMovement expose the location of the new movement`() {
		// Arrange
		val createdId = UUID.randomUUID()
		whenever(service.createMovement(any(), any(), any())).thenReturn(Mono.just(MovementModel(contentType = GUEST)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = GUEST).apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder("$BASE_URL/guests", listOf(defaultProjectId), emptyList()))
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		result.expectHeader().valueEquals("Location", "/api/v2/projects/$defaultProjectId/movements/$createdId")
		result.expectStatus().isCreated
	}

	@Test
	fun `Should updateGuestsMovementById throw when the movement changed since it was read`() {
		// Arrange
		whenever(service.findMovementById(any(), any(), anyOrNull())).thenReturn(Mono.just(guestModel()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/guests/{id}", listOf(defaultProjectId, UUID.randomUUID()), emptyList()))
			.header("If-Match", "\"1\"")
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		result.assertError(PRECONDITION_FAILED, RESOURCE_MODIFIED_SINCE_READ)

		verify(service, never()).updateMovementById(any(), any(), any(), any(), any())
	}

	@Test
	fun `Should updateGuestsMovementById accept the matching If-Match`() {
		// Arrange
		whenever(service.findMovementById(any(), any(), anyOrNull())).thenReturn(Mono.just(guestModel()))
		whenever(service.updateMovementById(any(), any(), any(), any(), any())).thenReturn(Mono.just(guestModel()))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = GUEST))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/guests/{id}", listOf(defaultProjectId, UUID.randomUUID()), emptyList()))
			.header("If-Match", "\"1791885731000\"")
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).updateMovementById(any(), any(), any(), any(), any())
	}

	@Test
	fun `Should createMovement refuse more than 200 contents`() {
		// Arrange
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = List(201) { ParticipantMovementContentWriterDto(participantId = UUID.randomUUID()) },
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createMovement refuse a pool name over 150 characters`() {
		// Arrange
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(
				ParticipantMovementContentWriterDto(
					participantId = UUID.randomUUID(),
					poolName = "p".repeat(151)
				)
			),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(defaultProjectId), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PARAMETER_TOO_LONG)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createGuestsMovement refuse more than 200 guests`() {
		// Arrange
		val guests = List(201) {
			GuestMovementWriterDto.GuestWriterDto(
				firstName = "Ada",
				lastName = "Lovelace",
				birthday = LocalDate.of(2010, 1, 1)
			)
		}
		val body = GuestMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = MovementReasonEnum.VISIT,
			content = null,
			guests = guests
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder("$BASE_URL/guests", listOf(defaultProjectId), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should createGuestsMovement refuse more than 200 contents`() {
		// Arrange
		val content = List(201) { MovementContentWriterDto(participantId = UUID.randomUUID()) }
		val body = GuestMovementWriterDto(dateTime = now(), type = OUT, reason = null, guests = null, content = content)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder("$BASE_URL/guests", listOf(defaultProjectId), emptyList()))
			.bodyValue(body)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, LIST_TOO_LARGE)
		verifyNoInteractions(service)
	}
}
