package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class ParticipantTimelineReaderDto(
	val planned: List<TimelineRangeReaderDto>,
	val actual: List<OutingReaderDto>,
)
