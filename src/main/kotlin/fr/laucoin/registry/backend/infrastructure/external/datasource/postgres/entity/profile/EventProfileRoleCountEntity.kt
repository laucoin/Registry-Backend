package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.EventProfileFields.ROLE_COUNT
import java.util.UUID
import org.springframework.data.relational.core.mapping.Column

data class EventProfileRoleCountEntity(
    @Column(LINKED_EVENT_ID)
    var eventId: UUID? = null,
    @Column(LINKED_EVENT_NAME)
    var eventName: String? = null,
    @Column(ROLE_COUNT)
    var level0: Int? = null,
)
