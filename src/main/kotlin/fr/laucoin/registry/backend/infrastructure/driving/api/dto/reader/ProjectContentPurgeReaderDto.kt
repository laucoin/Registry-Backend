package fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader

import java.util.UUID

data class ProjectContentPurgeReaderDto(
	var movements: List<UUID> = emptyList(),
	var alerts: List<UUID> = emptyList(),
	var communications: List<UUID> = emptyList(),
)
