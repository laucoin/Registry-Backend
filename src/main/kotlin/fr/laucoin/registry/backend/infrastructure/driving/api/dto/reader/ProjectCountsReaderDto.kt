package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

data class ProjectCountsReaderDto(
	var participants: Long = 0,
	var groups: Long = 0,
	var acceptedProfiles: Long = 0,
	var vehicles: Long? = null,
	var activities: Long? = null,
	var activeAlerts: Long? = null,
)
