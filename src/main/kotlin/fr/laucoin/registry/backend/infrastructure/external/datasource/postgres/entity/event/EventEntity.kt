package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_BEGIN_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventFields.EVENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEntity
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(EVENT_TABLE)
data class EventEntity(
    @Column(EVENT_NAME)
    var name: String? = null,
    @Column(EVENT_BEGIN_DATE)
    var beginDate: LocalDate? = null,
    @Column(EVENT_BEGIN_TIME)
    var beginTime: LocalTime? = null,
    @Column(EVENT_END_DATE)
    var endDate: LocalDate? = null,
    @Column(EVENT_END_TIME)
    var endTime: LocalTime? = null,
    @Column(EVENT_OPTIONS)
    var options: List<EventOptionEnum>? = null,
): GenericEntity()
