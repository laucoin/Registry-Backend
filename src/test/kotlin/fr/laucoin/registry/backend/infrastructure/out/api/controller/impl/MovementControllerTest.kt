package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_AND_REASON_ARE_DEFINED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_EMPTY
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CONTENT_PARTICIPANT_ID_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_AND_REASON_ARE_INCOMPATIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_TYPE_NULL
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_LOWER_THAN_ONE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_SIZE_IS_UPPER_THAN_MAX_PAGE_SIZE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.OTHER
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum.REASON
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.MovementTypeReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer.ParticipantMovementWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.stream.Stream
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
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.shaded.com.google.common.net.HttpHeaders.ACCEPT_LANGUAGE
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuples

class MovementControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IMovementService

	@MockitoBean
	private lateinit var readerMapper: MovementReaderDtoMapper

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

	companion object {
		private const val BASE_URL = "/api/projects/{projectId}/movements"

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
		pageNumber: Int?,
		pageSize: Int?,
		visibilitySearched: Boolean?,
		typeSearched: MovementTypeEnum?,
		startDateTimeSearched: String?,
		endDateTimeSearched: String?,
	) {
		// Arrange
		val expectedPageNumber = pageNumber ?: 0
		val expectedPageSize = pageSize ?: 20
		val pageable = PageableModel(expectedPageNumber * expectedPageSize, expectedPageSize)
		val searchParams = MovementSearchParamModel(
			visibilitySearched = visibilitySearched,
			typeSearched = typeSearched,
			startDateTimeSearched = startDateTimeSearched?.let {
				ZonedDateTime.parse(
					it,
					DateTimeFormatter.ISO_DATE_TIME
				)
			},
			endDateTimeSearched = endDateTimeSearched?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) },
		)
		val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findMovementsPage(any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDtoPage(any(), any())).thenReturn(
			PageModel(pageable, totalElements = 1, listOf(MovementReaderDto(contentType = REGISTERED))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
						Pair("visibilitySearched", visibilitySearched),
						Pair("typeSearched", typeSearched),
						Pair("startDateTimeSearched", startDateTimeSearched),
						Pair("endDateTimeSearched", endDateTimeSearched),
					),
				)
			)
			.header(ACCEPT_LANGUAGE, requestedLocale)
			.exchange()

		// Assert
		result.body<PageModel<*>>(OK)

		verify(service).findMovementsPage(projectId, pageable, searchParams)
		verify(readerMapper).toDtoPage(any(), any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should findMovements throw due to wrong params`(
		pageNumber: Int?,
		pageSize: Int?,
		expectedMessage: String,
	) {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("pageNumber", pageNumber),
						Pair("pageSize", pageSize),
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
		whenever(readerContentMapper.toDto(any(), any())).thenReturn(MovementContentReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/contents", listOf(projectId), listOf(Pair("movementIds", uuid))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).findMovementsContent(projectId, listOf(uuid))
		verifyNoInteractions(readerMapper)
		verify(readerContentMapper).toDto(any(), any())
		verifyNoInteractions(writerMapper)
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
		whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).findMovementById(projectId, uuid, visibilitySearched = null)
		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchReasonsAndActivities return 200`() {
		// Arrange
		val searched = "text"
		val typeSearched = IN
		val contentTypeSearched = REGISTERED
		whenever(service.searchActivitiesByText(any(), any(), anyOrNull())).thenReturn(Flux.just(ActivityModel()))
		whenever(service.searchReasonsByText(any(), any())).thenReturn(Flux.just(OTHER))
		whenever(activityReasonReaderMapper.toDto(any(), any())).thenReturn(
			MovementReasonsReaderDto(
				value = "value",
				label = "label",
				kind = REASON,
				type = IN,
			)
		)
		whenever(reasonReaderMapper.toDto(any(), any())).thenReturn(
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
					listOf(projectId),
					listOf(
						Pair("typeSearched", typeSearched),
						Pair("contentTypeSearched", contentTypeSearched),
						Pair("textSearched", searched)
					)
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchActivitiesByText(projectId, contentTypeSearched, searched)
		verify(service).searchReasonsByText(contentTypeSearched, typeSearched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(vehicleReaderMapper)
		verify(activityReasonReaderMapper).toDto(any(), any())
		verify(reasonReaderMapper, atLeastOnce()).toDto(any(), any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should searchParticipantsAndGroups return 200`() {
		// Arrange
		val searched = "text"
		val typeSearched = REGISTERED
		whenever(service.searchParticipantsAndGroupsByText(any(), any(), anyOrNull())).thenReturn(
			Mono.just(
				Tuples.of(
					listOf(ParticipantModel()),
					listOf(GroupModel()),
				)
			)
		)
		whenever(movementParticipantsAndGroupsReaderMapper.toDto(any(), any())).thenReturn(
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
					listOf(projectId),
					listOf(Pair("contentTypeSearched", typeSearched), Pair("textSearched", searched))
				)
			)
			.exchange()

		// Assert
		result.body<MovementParticipantsAndGroupsReaderDto>(OK)

		verify(service).searchParticipantsAndGroupsByText(projectId, typeSearched, searched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verify(movementParticipantsAndGroupsReaderMapper).toDto(any(), any())
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
		whenever(vehicleReaderMapper.toDto(any(), any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R),
				buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE)
			)
			.get()
			.uri(uriBuilder("$BASE_URL/search/vehicles", listOf(projectId), listOf(Pair("textSearched", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchVehiclesByText(projectId, searched)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(vehicleReaderMapper).toDto(any(), any())
		verifyNoInteractions(activityReasonReaderMapper)
		verifyNoInteractions(reasonReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should createMovement return 200`() {
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
			service.createMovement(
				any(),
				any(),
				any()
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).createMovement(any(), any(), any())
		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(writerMapper).toModel(any(), eq(projectId))
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
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
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
		whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verify(writerMapper).toModel(any(), eq(projectId))
		verify(service).updateMovementById(any(), eq(projectId), eq(uuid), any(), any())
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
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
				eq(projectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).disableMovementById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any(), any())
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
				eq(projectId),
				eq(uuid)
			)
		).thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any(), any())).thenReturn(MovementReaderDto(contentType = REGISTERED))


		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).enableMovementById(any(), eq(projectId), eq(uuid))
		verify(readerMapper).toDto(any(), any())
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should deleteMovementById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()

		whenever(service.deleteMovementById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)

		verify(service).deleteMovementById(projectId, uuid)
		verifyNoInteractions(readerMapper)
		verifyNoInteractions(movementTypeReaderMapper)
		verifyNoInteractions(movementParticipantsAndGroupsReaderMapper)
		verifyNoInteractions(writerMapper)
	}
}
