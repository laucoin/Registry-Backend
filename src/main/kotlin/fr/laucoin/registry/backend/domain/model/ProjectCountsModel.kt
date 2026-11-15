package fr.laucoin.registry.backend.domain.model

data class ProjectCountsModel(
	val participants: Long,
	val groups: Long,
	val acceptedProfiles: Long,
	val vehicles: Long? = null,
	val activities: Long? = null,
	val activeAlerts: Long? = null,
)
