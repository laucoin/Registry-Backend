package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity
import java.time.ZonedDateTime
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(EVENT_TABLE)
data class EventEntity(
    @Column(EVENT_NAME)
    var name: String? = null,
    @Column(EVENT_BEGIN)
    var begin: ZonedDateTime? = null,
    @Column(EVENT_END)
    var end: ZonedDateTime? = null,
    @Column(EVENT_OPTIONS)
    var options: List<EventOptionEnum>? = null,
): GenericEntity()
