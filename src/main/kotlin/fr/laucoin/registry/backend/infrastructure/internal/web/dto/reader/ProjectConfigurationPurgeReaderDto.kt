package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import java.util.UUID

data class ProjectConfigurationPurgeReaderDto(
    var vehicles: List<UUID> = emptyList(),
    var participants: List<UUID> = emptyList(),
    var activities: List<UUID> = emptyList(),
    var groups: List<UUID> = emptyList(),
)
