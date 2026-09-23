package fr.laucoin.registry.backend.domain.model

data class UserDataExportModel(
	var user: UserModel? = null,
	var preferences: PreferencesModel? = null,
	var projectProfiles: List<ProjectProfileModel> = emptyList(),
	var createdProjectProfiles: List<ProjectProfileModel> = emptyList(),
	var movements: List<MovementModel> = emptyList(),
	var communications: List<CommunicationModel> = emptyList(),
	var alerts: List<AlertModel> = emptyList(),
	var activities: List<ActivityModel> = emptyList(),
	var vehicles: List<VehicleModel> = emptyList(),
	var groups: List<GroupModel> = emptyList(),
	var linkedParticipants: List<ParticipantDataExportModel> = emptyList(),
)
