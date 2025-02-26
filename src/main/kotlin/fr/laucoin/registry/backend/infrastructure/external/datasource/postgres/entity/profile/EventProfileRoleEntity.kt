package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_VISIBLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.EVENT_PROFILE_ROLE
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

data class EventProfileRoleEntity(
    @Column(LINKED_EVENT_ID)
    var eventId: UUID? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_OPTIONS)
    var eventOptions: List<EventOptionEnum>? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_VISIBLE)
    var eventVisible: Boolean? = null,
    @Column(EVENT_PROFILE_ROLE)
    var role: String? = null,
)
