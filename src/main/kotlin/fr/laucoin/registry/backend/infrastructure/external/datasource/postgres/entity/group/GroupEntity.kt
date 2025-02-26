package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(GROUP_TABLE)
data class GroupEntity(
    @Column(GROUP_NAME)
    var name: String? = null,
    @Column(GROUP_START_AVAILABILITY_DATE)
    var startAvailabilityDate: LocalDate? = null,
    @Column(GROUP_START_AVAILABILITY_TIME)
    var startAvailabilityTime: LocalTime? = null,
    @Column(GROUP_END_AVAILABILITY_DATE)
    var endAvailabilityDate: LocalDate? = null,
    @Column(GROUP_END_AVAILABILITY_TIME)
    var endAvailabilityTime: LocalTime? = null,
): GenericEventEntity()
