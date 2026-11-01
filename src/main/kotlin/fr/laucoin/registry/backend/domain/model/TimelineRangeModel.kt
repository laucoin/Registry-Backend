package fr.laucoin.registry.backend.domain.model

data class TimelineRangeModel(
	val start: CustomDateTimeModel? = null,
	val end: CustomDateTimeModel? = null,
)
