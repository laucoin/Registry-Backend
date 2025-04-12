package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_END_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_END_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_OPTIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_START_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_START_TIME
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

abstract class GenericEventEntity(
    @Column(LINKED_EVENT_ID)
    var eventId: UUID? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_NAME)
    var eventName: String? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_START_DATE)
    var eventStartDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_START_TIME)
    var eventStartTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_END_DATE)
    var eventEndDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_END_TIME)
    var eventEndTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(LINKED_EVENT_OPTIONS)
    var eventOptions: List<EventOptionEnum>? = null,
): GenericEntity()
