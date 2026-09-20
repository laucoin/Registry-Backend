package fr.laucoin.registry.backend.domain.model

data class ParticipantDataExportModel(
	var participant: ParticipantModel? = null,
	var movements: List<MovementModel> = emptyList(),
	var communications: List<CommunicationModel> = emptyList(),
)
