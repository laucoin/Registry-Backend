package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class OngoingActivityOutingReaderDto(
	var movement: MovementReaderDto? = null,
	var recentCommunications: List<CommunicationReaderDto> = emptyList(),
)
