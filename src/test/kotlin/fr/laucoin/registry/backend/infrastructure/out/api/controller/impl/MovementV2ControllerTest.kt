package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.constant.ApiConst.DEFAULT_COLLECTION_LIMIT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_IDS_SIZE_IS_UPPER_THAN_MAX
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_IS_UNKNOWN
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_C
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_D
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_METADATA_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_R
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_MOVEMENT_U
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ACTIVITY
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_VEHICLE
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_R
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.OTHER
import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum.DATE_TIME
import fr.laucoin.registry.backend.domain.enumeration.MovementSortFieldEnum.TYPE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementContentsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.MovementContentWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import fr.laucoin.registry.backend.test.ModelExt.commonActivity
import fr.laucoin.registry.backend.test.ModelExt.commonCommunication
import fr.laucoin.registry.backend.test.ModelExt.commonGroup
import fr.laucoin.registry.backend.test.ModelExt.commonMovement
import fr.laucoin.registry.backend.test.ModelExt.commonParticipant
import fr.laucoin.registry.backend.test.ModelExt.commonVehicle
import fr.laucoin.registry.backend.test.ModelExt.movementId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.assertError
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.body
import fr.laucoin.registry.backend.test.WebTestClientExt.buildAuthority
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuples
import java.time.ZonedDateTime.now
import java.util.UUID
import kotlin.test.assertEquals

class MovementV2ControllerTest : TestContext() {
	@MockitoBean
	private lateinit var service: IMovementService

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v2/projects/{projectId}/movements"
	}

	private fun movementPage(): Mono<PageModel<MovementModel>> =
		Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonMovement())))

	@Test
	fun `Should findMovements return 200 with the v2 list grammar`() {
		// Arrange
		whenever(service.findMovementsPage(any(), any(), any(), any())).thenReturn(movementPage())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(
				uriBuilder(
					BASE_URL,
					listOf(projectId),
					listOf(
						Pair("page", 2),
						Pair("size", 10),
						Pair("visible", true),
						Pair("sort", "-dateTime,type"),
					),
				)
			)
			.exchange()

		// Assert
		result.body<PageModel<MovementReaderDto>>(OK)
		val pageableCaptor = argumentCaptor<PageableModel>()
		val sortCaptor = argumentCaptor<List<SortModel<MovementSortFieldEnum>>>()
		verify(service).findMovementsPage(eq(projectId), pageableCaptor.capture(), any(), sortCaptor.capture())
		assertEquals(PageableModel(offset = 20, limit = 10), pageableCaptor.firstValue)
		assertEquals(listOf(SortModel(DATE_TIME, descending = true), SortModel(TYPE)), sortCaptor.firstValue)
	}

	@Test
	fun `Should findMovements reject an unknown sort field with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), listOf(Pair("sort", "creatorId"))))
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, SORT_FIELD_IS_UNKNOWN)
	}

	@Test
	fun `Should findMovements return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should disableMovementById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.disableMovementById(any(), any(), any())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/disable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)
		verify(service).disableMovementById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should findEligibleVehicles expose the eligibility sub-collection with q`() {
		// Arrange
		whenever(service.searchVehiclesByText(any(), anyOrNull())).thenReturn(Flux.just(commonVehicle()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(uriBuilder("$BASE_URL/eligible-vehicles", listOf(projectId), listOf(Pair("q", "clio"))))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchVehiclesByText(projectId, "clio")
	}

	private fun participantMovement() = ParticipantMovementWriterDto(
		dateTime = now(),
		type = IN,
		reason = null,
		activityId = null,
		content = listOf(ParticipantMovementContentWriterDto(participantId = UUID.randomUUID())),
	)

	private fun guestMovement() = GuestMovementWriterDto(
		dateTime = now(),
		type = OUT,
		reason = null,
		content = listOf(MovementContentWriterDto(participantId = UUID.randomUUID())),
		guests = null,
	)

	@Test
	fun `Should findMovementById return 200 with the mapped movement`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findMovementById(any(), any(), anyOrNull())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		val movement = result.body<MovementReaderDto>(OK)
		assertEquals(movementId, movement?.id)
		verify(service).findMovementById(projectId, id, visibilitySearched = null)
	}

	@Test
	fun `Should findMovementById return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findMovementsContents return 200 with the mapped contents`() {
		// Arrange
		whenever(service.findMovementsContent(any(), any())).thenReturn(
			Flux.just(Pair(movementId, listOf(MovementModel.MovementContentModel()))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/contents", listOf(projectId), listOf(Pair("movementIds", movementId))))
			.exchange()

		// Assert
		val contents = result.body<List<MovementContentsReaderDto>>(OK)
		assertEquals(movementId, contents?.first()?.movementId)
		verify(service).findMovementsContent(projectId, listOf(movementId))
	}

	@Test
	fun `Should findMovementsContents reject more than 200 movementIds with 400`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/contents",
					listOf(projectId),
					(1..201).map { Pair("movementIds", UUID.randomUUID()) })
			)
			.exchange()

		// Assert
		result.assertError(BAD_REQUEST, MOVEMENT_IDS_SIZE_IS_UPPER_THAN_MAX)
		verifyNoInteractions(service)
	}

	@Test
	fun `Should findMovementsContents return 403 without the read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/contents", listOf(projectId), listOf(Pair("movementIds", movementId))))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findReasons return 200 with reasons and activities merged`() {
		// Arrange
		whenever(service.searchActivitiesByText(any(), any(), anyOrNull())).thenReturn(Flux.just(commonActivity()))
		whenever(service.searchReasonsByText(any(), any())).thenReturn(Flux.just(OTHER))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/reasons",
					listOf(projectId),
					listOf(Pair("type", IN), Pair("contentType", REGISTERED)),
				)
			)
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).searchActivitiesByText(projectId, REGISTERED, null)
		verify(service).searchReasonsByText(REGISTERED, IN)
	}

	@Test
	fun `Should findReasons return 403 without the metadata authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/reasons",
					listOf(projectId),
					listOf(Pair("type", IN), Pair("contentType", REGISTERED)),
				)
			)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findEligibleParticipantsAndGroups return 200 with the eligibility sub-collection`() {
		// Arrange
		whenever(service.searchParticipantsAndGroupsByText(any(), any(), anyOrNull())).thenReturn(
			Mono.just(Tuples.of(listOf(commonParticipant()), listOf(commonGroup()))),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_METADATA_R))
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/eligible-participants-and-groups",
					listOf(projectId),
					listOf(Pair("contentType", REGISTERED), Pair("q", "john")),
				)
			)
			.exchange()

		// Assert
		result.body<MovementParticipantsAndGroupsReaderDto>(OK)
		verify(service).searchParticipantsAndGroupsByText(projectId, REGISTERED, "john")
	}

	@Test
	fun `Should findEligibleParticipantsAndGroups return 403 without the metadata authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(
				uriBuilder(
					"$BASE_URL/eligible-participants-and-groups",
					listOf(projectId),
					listOf(Pair("contentType", REGISTERED)),
				)
			)
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findMovementCommunications return 200 with the communications page`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.findMovementCommunicationsPage(any(), any(), any(), any())).thenReturn(
			Mono.just(PageModel(PageableModel(0, 20), 1, listOf(commonCommunication()))),
		)

		// Act
		val result = webClient
			.authenticate(
				buildAuthority(REGISTRY_PROJECT_OPTION_COMMUNICATION),
				buildAuthority(REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R),
			)
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/communications", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<PageModel<CommunicationReaderDto>>(OK)
		verify(service).findMovementCommunicationsPage(eq(projectId), eq(id), eq(PageableModel(0, 20)), any())
	}

	@Test
	fun `Should findMovementCommunications return 403 without the communication option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_COMMUNICATION_R))
			.get()
			.uri(uriBuilder("$BASE_URL/{id}/communications", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findParticipantsStatus return 200 with the presence status`() {
		// Arrange
		whenever(service.findParticipantsStatus(any())).thenReturn(
			Mono.just(ProjectStatusModel(ProjectStatusModel.ParticipantStatusModel(1, 2, 3, 4), guests = 5)),
		)

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/participants/status", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		val status = result.body<ProjectStatusModel>(OK)
		assertEquals(5L, status?.guests)
		verify(service).findParticipantsStatus(projectId)
	}

	@Test
	fun `Should findParticipantsStatus return 403 without the project read authority`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder("$BASE_URL/participants/status", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findVehiclesStatus return 200 with the vehicles status`() {
		// Arrange
		whenever(service.findVehiclesStatus(any())).thenReturn(Mono.just(VehicleStatusModel(present = 1, absent = 2)))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_VEHICLE), buildAuthority(REGISTRY_PROJECT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/vehicles/status", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		val status = result.body<VehicleStatusModel>(OK)
		assertEquals(1L, status?.present)
		verify(service).findVehiclesStatus(projectId)
	}

	@Test
	fun `Should findVehiclesStatus return 403 without the vehicle option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/vehicles/status", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should findOngoingActivities pass the default limit to the service`() {
		// Arrange
		whenever(service.findOngoingActivities(any(), any())).thenReturn(Flux.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_OPTION_ACTIVITY), buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/activities/ongoing", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.body<List<*>>(OK)
		verify(service).findOngoingActivities(projectId, DEFAULT_COLLECTION_LIMIT)
	}

	@Test
	fun `Should findOngoingActivities return 403 without the activity option`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_R))
			.get()
			.uri(uriBuilder("$BASE_URL/activities/ongoing", listOf(projectId), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createMovement return 200 and delegate to the service`() {
		// Arrange
		whenever(service.createMovement(any(), any(), any())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participantMovement())
			.exchange()

		// Assert
		val movement = result.body<MovementReaderDto>(OK)
		assertEquals(movementId, movement?.id)
		verify(service).createMovement(any(), any(), any())
	}

	@Test
	fun `Should createMovement return 403 without the create authority`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder(BASE_URL, listOf(projectId), emptyList()))
			.bodyValue(participantMovement())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should createGuestsMovement return 200 and delegate to the service`() {
		// Arrange
		whenever(service.createMovement(any(), any(), any())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_C))
			.post()
			.uri(uriBuilder("$BASE_URL/guests", listOf(projectId), emptyList()))
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		val movement = result.body<MovementReaderDto>(OK)
		assertEquals(movementId, movement?.id)
		verify(service).createMovement(any(), any(), any())
	}

	@Test
	fun `Should createGuestsMovement return 403 without the create authority`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/guests", listOf(projectId), emptyList()))
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateMovementById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateMovementById(any(), any(), any(), any(), any())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(participantMovement())
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)
		verify(service).updateMovementById(any(), eq(projectId), eq(id), any(), any())
	}

	@Test
	fun `Should updateMovementById return 403 without the update authority`() {
		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.bodyValue(participantMovement())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should updateGuestsMovementById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.updateMovementById(any(), any(), any(), any(), any())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.patch()
			.uri(uriBuilder("$BASE_URL/guests/{id}", listOf(projectId, id), emptyList()))
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)
		verify(service).updateMovementById(any(), eq(projectId), eq(id), any(), any())
	}

	@Test
	fun `Should updateGuestsMovementById return 403 without the update authority`() {
		// Act
		val result = webClient
			.authenticate()
			.patch()
			.uri(uriBuilder("$BASE_URL/guests/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.bodyValue(guestMovement())
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should enableMovementById act as an explicit POST transition`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.enableMovementById(any(), any(), any())).thenReturn(Mono.just(commonMovement()))

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<MovementReaderDto>(OK)
		verify(service).enableMovementById(any(), eq(projectId), eq(id))
	}

	@Test
	fun `Should enableMovementById return 403 without the update authority`() {
		// Act
		val result = webClient
			.authenticate()
			.post()
			.uri(uriBuilder("$BASE_URL/{id}/enable", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}

	@Test
	fun `Should deleteMovementById return 200 and delegate to the service`() {
		// Arrange
		val id = UUID.randomUUID()
		whenever(service.deleteMovementById(any(), any())).thenReturn(Mono.empty())

		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_D))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, id), emptyList()))
			.exchange()

		// Assert
		result.body<Void>(OK)
		verify(service).deleteMovementById(projectId, id)
	}

	@Test
	fun `Should deleteMovementById return 403 without the delete authority`() {
		// Act
		val result = webClient
			.authenticate(buildAuthority(REGISTRY_PROJECT_MOVEMENT_U))
			.delete()
			.uri(uriBuilder("$BASE_URL/{id}", listOf(projectId, UUID.randomUUID()), emptyList()))
			.exchange()

		// Assert
		result.assertError(FORBIDDEN, NOT_ENOUGH_PERMISSION)
		verifyNoInteractions(service)
	}
}
