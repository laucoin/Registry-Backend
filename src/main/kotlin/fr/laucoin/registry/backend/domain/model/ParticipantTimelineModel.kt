package fr.laucoin.registry.backend.domain.model

data class ParticipantTimelineModel(
	val planned: List<TimelineRangeModel> = emptyList(),
	val actual: List<OutingModel> = emptyList(),
)
