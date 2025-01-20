package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import java.util.UUID

@JsonInclude(NON_NULL)
data class CreatedEventProfilesReaderDto(
    var profiles: List<EventProfileModel>,
    var notCreatedUserIds: List<UUID>? = null,
)
