package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import java.util.UUID

data class EventProfileRoleModel(
    var eventId: UUID? = null,
    var eventOptions: List<EventOptionEnum>? = null,
    var eventVisible: Boolean? = null,
    var role: String? = null,
)
