package fr.laucoin.registry.backend.domain.model

data class OngoingActivityOutingModel(
	var movement: MovementModel? = null,
	var recentCommunications: List<CommunicationModel> = emptyList(),
)
