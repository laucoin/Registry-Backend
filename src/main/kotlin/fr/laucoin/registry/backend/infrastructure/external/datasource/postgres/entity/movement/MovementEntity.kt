package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_REASON
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(MOVEMENT_TABLE)
data class MovementEntity(
    @Column(MOVEMENT_DATE_TIME)
    var dateTime: ZonedDateTime? = null,
    @Column(MOVEMENT_TYPE)
    var type: MovementTypeEnum? = null,

    @Column(MOVEMENT_REASON)
    var reason: MovementReasonEnum? = null,

    @Column(MOVEMENT_ACTIVITY_ID)
    var activityId: UUID? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_NAME)
    var activityName: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_DESCRIPTION)
    var activityDescription: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_DURATION)
    var activityDuration: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_MIN_ALLOWED_PARTICIPANTS)
    var activityMinAllowedParticipants: Int? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_MAX_ALLOWED_PARTICIPANTS)
    var activityMaxAllowedParticipants: Int? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_START_AVAILABILITY_DATE)
    var activityStartAvailabilityDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_START_AVAILABILITY_TIME)
    var activityStartAvailabilityTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_END_AVAILABILITY_DATE)
    var activityEndAvailabilityDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_ACTIVITY_END_AVAILABILITY_TIME)
    var activityEndAvailabilityTime: LocalTime? = null,
): GenericEventEntity()
