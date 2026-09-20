package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class UserDataExportReaderDto(
	var user: UserReaderDto? = null,
	var preferences: PreferencesReaderDto? = null,
	var projectProfiles: List<ProjectProfileReaderDto> = emptyList(),
	var createdProjectProfiles: List<ProjectProfileReaderDto> = emptyList(),
	var movements: List<MovementReaderDto> = emptyList(),
	var communications: List<CommunicationReaderDto> = emptyList(),
	var alerts: List<AlertReaderDto> = emptyList(),
	var activities: List<ActivityReaderDto> = emptyList(),
	var vehicles: List<VehicleReaderDto> = emptyList(),
	var groups: List<GroupWithoutMemberReaderDto> = emptyList(),
	var linkedParticipants: List<ParticipantDataExportReaderDto> = emptyList(),
)
