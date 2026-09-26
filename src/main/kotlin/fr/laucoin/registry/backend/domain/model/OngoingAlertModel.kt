package fr.laucoin.registry.backend.domain.model

data class OngoingAlertModel(
	var alert: AlertModel? = null,
	var recentCommunications: List<CommunicationModel> = emptyList(),
)
