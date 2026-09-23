package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantDataExportModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.port.IUserPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.IParticipantService
import fr.laucoin.registry.backend.domain.service.IUserDataExportService
import fr.laucoin.registry.backend.test.ModelExt.commonParticipant
import fr.laucoin.registry.backend.test.ModelExt.commonProjectProfile
import fr.laucoin.registry.backend.test.ModelExt.commonUser
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class UserDataExportServiceTest {
	private val userPort: IUserPort = mock()
	private val preferencesPort: IPreferencesPort = mock()
	private val projectProfilePort: IProjectProfilePort = mock()
	private val movementPort: IMovementPort = mock()
	private val communicationPort: ICommunicationPort = mock()
	private val alertPort: IAlertPort = mock()
	private val activityPort: IActivityPort = mock()
	private val vehiclePort: IVehiclePort = mock()
	private val groupPort: IGroupPort = mock()
	private val participantPort: IParticipantPort = mock()
	private val participantService: IParticipantService = mock()
	private val service: IUserDataExportService = UserDataExportService(
		userPort,
		preferencesPort,
		projectProfilePort,
		movementPort,
		communicationPort,
		alertPort,
		activityPort,
		vehiclePort,
		groupPort,
		participantPort,
		participantService,
	)

	@Test
	fun `Should exportCurrentUserData gather everything owned or authored by the caller`() {
		// Arrange
		val user = commonUser()
		val preferences = PreferencesModel()
		val ownProfile = commonProjectProfile()
		val createdProfile = commonProjectProfile()
		val movement = MovementModel()
		val communication = CommunicationModel()
		val alert = AlertModel()
		val activity = ActivityModel()
		val vehicle = VehicleModel()
		val group = GroupModel()
		val linkedParticipant = commonParticipant()
		val linkedParticipantExport = ParticipantDataExportModel(participant = linkedParticipant)

		whenever(userPort.findById(any(), any())).thenReturn(Mono.just(user))
		whenever(preferencesPort.findByUserId(any(), any())).thenReturn(Mono.just(preferences))
		whenever(projectProfilePort.findProjectProfilesPageByUserId(any(), any(), any(), any()))
			.thenReturn(Mono.just(PageModel(PageableModel(0, 10_000), 1, listOf(ownProfile))))
		whenever(projectProfilePort.findAllByCreatorId(any())).thenReturn(Flux.just(createdProfile))
		whenever(movementPort.findAllByCreatorId(any())).thenReturn(Flux.just(movement))
		whenever(communicationPort.findAllByCreatorId(any())).thenReturn(Flux.just(communication))
		whenever(alertPort.findAllByCreatorId(any())).thenReturn(Flux.just(alert))
		whenever(activityPort.findAllByCreatorId(any())).thenReturn(Flux.just(activity))
		whenever(vehiclePort.findAllByCreatorId(any())).thenReturn(Flux.just(vehicle))
		whenever(groupPort.findAllByCreatorId(any())).thenReturn(Flux.just(group))
		whenever(participantPort.findAllByUserId(any())).thenReturn(Flux.just(linkedParticipant))
		whenever(participantService.exportParticipantData(any(), any())).thenReturn(Mono.just(linkedParticipantExport))

		// Act
		val result = service.exportCurrentUserData(currentUser()).block()

		// Assert
		assertEquals(user, result?.user)
		assertEquals(preferences, result?.preferences)
		assertEquals(listOf(ownProfile), result?.projectProfiles)
		assertEquals(listOf(createdProfile), result?.createdProjectProfiles)
		assertEquals(listOf(movement), result?.movements)
		assertEquals(listOf(communication), result?.communications)
		assertEquals(listOf(alert), result?.alerts)
		assertEquals(listOf(activity), result?.activities)
		assertEquals(listOf(vehicle), result?.vehicles)
		assertEquals(listOf(group), result?.groups)
		assertEquals(listOf(linkedParticipantExport), result?.linkedParticipants)
	}
}
