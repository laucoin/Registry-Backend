package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PAGE_NUMBER_IS_LOWER_THAN_ZERO
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.OTHER
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum.REASON
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.CommunicationReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementActivityReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementContentReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementParticipantsAndGroupsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.MovementReasonReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader.VehicleReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer.ParticipantMovementWriterDtoMapper
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import java.time.ZonedDateTime.now
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuples

class MovementV2ControllerTest: TestContext() {
	@MockitoBean
	private lateinit var service: IMovementService

	@MockitoBean
	private lateinit var readerMapper: MovementReaderDtoMapper

	@MockitoBean
	private lateinit var readerContentMapper: MovementContentReaderDtoMapper

	@MockitoBean
	private lateinit var reasonReaderMapper: MovementReasonReaderDtoMapper

	@MockitoBean
	private lateinit var activityReasonReaderMapper: MovementActivityReasonReaderDtoMapper

	@MockitoBean
	private lateinit var communicationReaderMapper: CommunicationReaderDtoMapper

	@MockitoBean
	private lateinit var movementParticipantsAndGroupsMapper: MovementParticipantsAndGroupsReaderDtoMapper

	@MockitoBean
	private lateinit var vehiclesMapper: VehicleReaderDtoMapper

	@MockitoBean
	private lateinit var writerMapper: ParticipantMovementWriterDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/movements"
	}

	@Test
	fun `Should findMovements drop the Searched suffix and call service`() {
		// Arrange
		val pageable = PageableModel(0, 20)
		val searchParams = MovementSearchParamModel(visibilitySearched = true, typeSearched = IN)
		val page = PageModel(pageable, totalElements = 1, listOf(MovementModel(contentType = REGISTERED)))
		whenever(service.findMovementsPage(any(), any(), any())).thenReturn(Mono.just(page))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("visible", true), Pair("type", IN))))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findMovementsPage(projectId, pageable, searchParams)
		verify(readerMapper, atLeastOnce()).toDto(any())
		verifyNoInteractions(writerMapper)
	}

	@Test
	fun `Should findMovements return 400 when a query param is invalid`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("page", -1))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, PAGE_NUMBER_IS_LOWER_THAN_ZERO)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findMovementsContents return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findMovementsContent(any(), any())).thenReturn(
			Flux.just(Pair(uuid, listOf(MovementModel.MovementContentModel())))
		)
		whenever(readerContentMapper.toDto(any())).thenReturn(MovementContentReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/contents", listOf(projectId), listOf(Pair("movementIds", uuid))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).findMovementsContent(projectId, listOf(uuid))
		verify(readerContentMapper).toDto(any())
	}

	@Test
	fun `Should findMovementById return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.findMovementById(any(), any(), anyOrNull()))
			.thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).findMovementById(projectId, uuid, visibilitySearched = null)
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should searchReasonsAndActivities drop the Searched suffix and call service`() {
		// Arrange
		val searched = "text"
		whenever(service.searchActivitiesByText(any(), any(), anyOrNull())).thenReturn(Flux.just(ActivityModel()))
		whenever(service.searchReasonsByText(any(), any())).thenReturn(Flux.just(OTHER))
		whenever(activityReasonReaderMapper.toDto(any())).thenReturn(MovementReasonsReaderDto("value", "label", REASON, IN))
		whenever(reasonReaderMapper.toDto(any())).thenReturn(MovementReasonsReaderDto("value", "label", REASON, IN))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/search/reasons",
					listOf(projectId),
					listOf(Pair("type", IN), Pair("contentType", REGISTERED), Pair("q", searched)),
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchActivitiesByText(projectId, REGISTERED, searched)
		verify(service).searchReasonsByText(REGISTERED, IN)
	}

	@Test
	fun `Should searchParticipantsAndGroups drop the Searched suffix and call service`() {
		// Arrange
		val searched = "text"
		whenever(service.searchParticipantsAndGroupsByText(any(), any(), anyOrNull()))
			.thenReturn(Mono.just(Tuples.of(listOf(ParticipantModel()), listOf(GroupModel()))))
		whenever(movementParticipantsAndGroupsMapper.toDto(any()))
			.thenReturn(MovementParticipantsAndGroupsReaderDto(emptyList(), emptyList()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/search/participants-and-groups",
					listOf(projectId),
					listOf(Pair("contentType", REGISTERED), Pair("q", searched)),
				)
			)
			.exchange()

		// Assert
		result.body<MovementParticipantsAndGroupsReaderDto>(OK)

		verify(service).searchParticipantsAndGroupsByText(projectId, REGISTERED, searched)
	}

	@Test
	fun `Should searchVehicles drop the Searched suffix and call service`() {
		// Arrange
		val searched = "text"
		whenever(service.searchVehiclesByText(any(), anyOrNull())).thenReturn(Flux.just(VehicleModel()))
		whenever(vehiclesMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R), buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE))
			.get()
			.uri(uriBuilder("$BASE_URL/search/vehicles", listOf(projectId), listOf(Pair("q", searched))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)

		verify(service).searchVehiclesByText(projectId, searched)
	}

	@Test
	fun `Should findMovementCommunications drop the Searched suffix and call service`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val pageable = PageableModel(0, 20)
		val searchParams = CommunicationSearchParamModel(visibilitySearched = true)
		val page = PageModel(pageable, totalElements = 0, emptyList<CommunicationModel>())
		whenever(service.findMovementCommunicationsPage(any(), any(), any(), any())).thenReturn(Mono.just(page))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION), buildAuthority(REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/communications", listOf(projectId, uuid), listOf(Pair("visible", true))))
			.exchange()

		// Assert
		result.body<Map<*, *>>(OK)

		verify(service).findMovementCommunicationsPage(projectId, uuid, pageable, searchParams)
	}

	@Test
	fun `Should findParticipantsStatus map ProjectStatusModel to a dedicated reader dto`() {
		// Arrange
		whenever(service.findParticipantsStatus(any())).thenReturn(
			Mono.just(ProjectStatusModel(ProjectStatusModel.ParticipantStatusModel(1, 2, 3, 4), guests = 5))
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/participants/status", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		val body = result.body<Map<*, *>>(OK)
		assertNotNull(body?.get("registered"))
		assertNotNull(body?.get("guests"))

		verify(service).findParticipantsStatus(projectId)
	}

	@Test
	fun `Should findVehiclesStatus map VehicleStatusModel to a dedicated reader dto`() {
		// Arrange
		whenever(service.findVehiclesStatus(any())).thenReturn(Mono.just(VehicleStatusModel(present = 1, absent = 2)))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/vehicles/status", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		val body = result.body<Map<*, *>>(OK)
		assertNotNull(body?.get("present"))
		assertNotNull(body?.get("absent"))

		verify(service).findVehiclesStatus(projectId)
	}

	@Test
	fun `Should createMovement return 201 with a Location header`() {
		// Arrange
		val uuid = UUID.randomUUID()
		val createdId = UUID.randomUUID()
		val movement = ParticipantMovementWriterDto(
			dateTime = now(),
			type = IN,
			reason = null,
			activityId = null,
			content = listOf(ParticipantMovementContentWriterDto(participantId = uuid)),
		)
		whenever(service.createMovement(any(), any(), any()))
			.thenReturn(Mono.just(MovementModel(contentType = REGISTERED).apply { id = createdId }))
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED).apply { id = createdId })

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		val entityResult = result.expectStatus().isCreated.expectBody(MovementReaderDto::class.java).returnResult()
		val location = entityResult.responseHeaders.getFirst("Location")
		assertNotNull(location)
		assertTrue(location!!.endsWith("/api/v2/projects/$projectId/movements/$createdId"))

		verify(service).createMovement(any(), any(), any())
		verify(readerMapper).toDto(any())
		verify(writerMapper).toModel(any(), eq(projectId))
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
			content = listOf(ParticipantMovementContentWriterDto(participantId = uuid)),
		)
		whenever(service.updateMovementById(any(), any(), any(), any(), any()))
			.thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(writerMapper.toModel(any(), any())).thenReturn(MovementModel(contentType = REGISTERED))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.bodyValue(movement)
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).updateMovementById(any(), eq(projectId), eq(uuid), any(), any())
		verify(readerMapper).toDto(any())
	}

	@Test
	fun `Should disableMovementById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.disableMovementById(any(), eq(projectId), eq(uuid)))
			.thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).disableMovementById(any(), eq(projectId), eq(uuid))
	}

	@Test
	fun `Should enableMovementById use POST and return 200`() {
		// Arrange
		val uuid = UUID.randomUUID()
		whenever(service.enableMovementById(any(), eq(projectId), eq(uuid)))
			.thenReturn(Mono.just(MovementModel(contentType = REGISTERED)))
		whenever(readerMapper.toDto(any())).thenReturn(MovementReaderDto(contentType = REGISTERED))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)

		verify(service).enableMovementById(any(), eq(projectId), eq(uuid))
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
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, uuid), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(NO_CONTENT)

		verify(service).deleteMovementById(projectId, uuid)
	}
}
