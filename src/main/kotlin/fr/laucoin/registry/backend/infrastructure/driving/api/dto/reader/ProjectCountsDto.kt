package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class ProjectCountsDto(
	var participants: Long = 0,
	var vehicles: Long = 0,
	var groups: Long = 0,
	var activities: Long = 0,
	var profiles: Long = 0,
	var ongoingAlerts: Long = 0,
)
