package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class ParticipantExportModel(
	val generatedAt: ZonedDateTime,
	val projectName: String?,
	val participant: ParticipantModel,
	val movements: List<MovementModel>,
)

data class UserExportModel(
	val generatedAt: ZonedDateTime,
	val user: UserModel,
	val preferences: PreferencesModel?,
	val projectProfiles: List<ProjectProfileModel>,
	val participants: List<ParticipantExportModel>,
)
