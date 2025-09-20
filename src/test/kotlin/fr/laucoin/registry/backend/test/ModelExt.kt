package fr.laucoin.registry.backend.test

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

object ModelExt {
	val projectId: UUID = UUID.fromString("b7432b97-cfc6-4109-aaaa-38d348523f1e")
	val projectProfileId: UUID = UUID.fromString("28d92461-addb-42d5-9301-18ef6e966608")
	val userId: UUID = UUID.fromString("9cd10ea7-96c1-4f82-8366-d11d2e3ec300")
	val userOidcId: UUID = UUID.fromString("08513d7b-cce1-4efe-bb27-8d936c4a12b4")
	val userIdWithoutProfile: UUID = UUID.fromString("e22a08da-b8b8-4b78-86c8-8557ddfbb945")
	val groupId: UUID = UUID.fromString("acb4943c-a911-4f1d-b899-69f6cfcfef90")
	val alertId: UUID = UUID.fromString("da5ae275-d828-4738-ac47-367fdad1bff4")
	val movementId: UUID = UUID.fromString("63f4c4e8-bd07-445b-8a6e-899ac490cf0c")
	val participantId: UUID = UUID.fromString("88f7194e-6633-4f84-b3e3-8546b51d07e0")
	val activityId: UUID = UUID.fromString("95806471-9c01-477a-84ea-8c37fd0cc8c5")
	val communicationId: UUID = UUID.fromString("64303545-0826-4efe-9f60-43b1219f75dc")
	val vehicleId: UUID = UUID.fromString("7ae25102-8337-4836-93e5-dd2cd8c5d5ec")

	fun commonProject() = ProjectModel().apply { id = projectId }
	fun commonProjectProfile() = ProjectProfileModel().apply {
		id = projectProfileId
		user = commonUser()
		project = commonProject()
	}

	fun commonUser() = UserModel().apply {
		id = userIdWithoutProfile
		lastLogin = ZonedDateTime.of(2020, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
	}

	fun commonGroup() = GroupModel().apply { id = groupId; project = commonProject() }
	fun commonAlert() = AlertModel().apply {
		id = alertId
		dateTime = ZonedDateTime.of(2020, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
		project = commonProject()
	}

	fun commonMovement() = MovementModel().apply {
		id = movementId
		dateTime = ZonedDateTime.of(2020, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
		project = commonProject()
	}

	fun commonParticipant() = ParticipantModel().apply { id = participantId; project = commonProject() }
	fun commonActivity() = ActivityModel().apply { id = activityId; project = commonProject() }
	fun commonCommunication() = CommunicationModel().apply {
		id = communicationId
		dateTime = ZonedDateTime.of(2020, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC)
		project = commonProject()
	}

	fun commonVehicle() = VehicleModel().apply { id = vehicleId; project = commonProject() }
}
