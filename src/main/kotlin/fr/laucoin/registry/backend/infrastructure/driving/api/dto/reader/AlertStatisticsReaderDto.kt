package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class AlertStatisticsReaderDto(
	val resolvedCount: Long,
	val canceledCount: Long,
	val averageResolutionSeconds: Long?,
	val realAlertRate: Double?,
)
