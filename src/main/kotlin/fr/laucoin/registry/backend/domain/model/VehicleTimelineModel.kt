package fr.laucoin.registry.backend.domain.model

data class VehicleTimelineModel(
	val planned: List<TimelineRangeModel> = emptyList(),
	val actual: List<OutingModel> = emptyList(),
	val outSeconds: Long = 0,
	val usageRate: Double? = null,
)
