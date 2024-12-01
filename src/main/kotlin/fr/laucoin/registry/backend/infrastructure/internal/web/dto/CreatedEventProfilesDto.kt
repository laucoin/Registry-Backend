package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import fr.laucoin.registry.backend.domain.model.EventProfileModel
import java.util.UUID

data class CreatedEventProfilesDto(
    var profiles: List<EventProfileModel>,
    var notCreatedUserIds: List<UUID>? = null,
)
