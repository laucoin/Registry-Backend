package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import java.time.LocalDate
import java.time.LocalTime
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(ACTIVITY_TABLE)
data class ActivityEntity(
    @Column(ACTIVITY_NAME)
    var name: String? = null,
    @Column(ACTIVITY_DESCRIPTION)
    var description: String? = null,
    @Column(ACTIVITY_DURATION)
    var duration: String? = null,
    @Column(ACTIVITY_MIN_ALLOWED_PARTICIPANTS)
    var minAllowedParticipants: Int? = null,
    @Column(ACTIVITY_MAX_ALLOWED_PARTICIPANTS)
    var maxAllowedParticipants: Int? = null,
    @Column(ACTIVITY_START_AVAILABILITY_DATE)
    var startAvailabilityDate: LocalDate? = null,
    @Column(ACTIVITY_START_AVAILABILITY_TIME)
    var startAvailabilityTime: LocalTime? = null,
    @Column(ACTIVITY_END_AVAILABILITY_DATE)
    var endAvailabilityDate: LocalDate? = null,
    @Column(ACTIVITY_END_AVAILABILITY_TIME)
    var endAvailabilityTime: LocalTime? = null,
): GenericEventEntity()
