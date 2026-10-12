package fr.laucoin.registry.backend.domain.model

import java.util.UUID

data class ProjectConfigurationPurgeModel(
	val vehicles: List<UUID>,
	val participants: List<UUID>,
	val activities: List<UUID>,
	val groups: List<UUID>,
)
