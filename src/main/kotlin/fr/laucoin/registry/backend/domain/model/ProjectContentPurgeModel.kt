package fr.laucoin.registry.backend.domain.model

import java.util.UUID

data class ProjectContentPurgeModel(
	val movements: List<UUID>,
	val alerts: List<UUID>,
	val communications: List<UUID>,
)
