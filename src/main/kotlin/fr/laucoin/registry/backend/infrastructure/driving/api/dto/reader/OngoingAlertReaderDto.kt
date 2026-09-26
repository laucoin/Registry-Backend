package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class OngoingAlertReaderDto(
	var alert: AlertReaderDto? = null,
	var recentCommunications: List<CommunicationReaderDto> = emptyList(),
)
