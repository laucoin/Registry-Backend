package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class VehicleTimelineReaderDto(
	val planned: List<TimelineRangeReaderDto>,
	val actual: List<OutingReaderDto>,
	val outSeconds: Long,
	val usageRate: Double?,
)
