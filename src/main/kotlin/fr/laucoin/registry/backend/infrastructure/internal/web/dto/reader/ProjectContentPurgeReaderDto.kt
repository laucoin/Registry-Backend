package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import java.util.UUID

data class ProjectContentPurgeReaderDto(
    var movements: List<UUID> = emptyList(),
    var alerts: List<UUID> = emptyList(),
    var communications: List<UUID> = emptyList(),
)
