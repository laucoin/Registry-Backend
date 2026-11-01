package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class ActivityStatisticsReaderDto(
	val participantCount: Long,
	val projectParticipantCount: Long,
	val participantRate: Double?,
	val movementSeconds: Long,
	val manSeconds: Long,
)
