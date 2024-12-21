package fr.laucoin.registry.backend.infrastructure.internal.web.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import java.util.UUID

@JsonInclude(NON_NULL)
data class CreatedEventProfilesDto(
    var profiles: List<EventProfileModel>,
    var notCreatedUserIds: List<UUID>? = null,
)
