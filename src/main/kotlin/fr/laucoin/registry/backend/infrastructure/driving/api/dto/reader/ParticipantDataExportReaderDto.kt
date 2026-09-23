package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class ParticipantDataExportReaderDto(
	var participant: ParticipantReaderDto? = null,
	var movements: List<MovementReaderDto> = emptyList(),
	var communications: List<CommunicationReaderDto> = emptyList(),
)
