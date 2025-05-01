package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_DESCRIPTION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_DURATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_MAX_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_MIN_ALLOWED_PARTICIPANTS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_ACTIVITY_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MESSAGE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MOVEMENT_REASON
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(COMMUNICATION_TABLE)
data class CommunicationEntity(
    @Column(COMMUNICATION_DATE_TIME)
    var dateTime: ZonedDateTime? = null,
    @Column(COMMUNICATION_MESSAGE)
    var message: String? = null,

    @Column(COMMUNICATION_MOVEMENT_ID)
    var movementId: UUID? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_MOVEMENT_DATE_TIME)
    var movementDateTime: ZonedDateTime? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_MOVEMENT_TYPE)
    var movementType: MovementTypeEnum? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_MOVEMENT_REASON)
    var movementReason: MovementReasonEnum? = null,

    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_ID)
    var activityId: UUID? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_NAME)
    var activityName: String? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_DESCRIPTION)
    var activityDescription: String? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_DURATION)
    var activityDuration: String? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_MIN_ALLOWED_PARTICIPANTS)
    var activityMinAllowedParticipants: Int? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_MAX_ALLOWED_PARTICIPANTS)
    var activityMaxAllowedParticipants: Int? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_START_AVAILABILITY_DATE)
    var activityStartAvailabilityDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_START_AVAILABILITY_TIME)
    var activityStartAvailabilityTime: LocalTime? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_END_AVAILABILITY_DATE)
    var activityEndAvailabilityDate: LocalDate? = null,
    @ReadOnlyProperty
    @Column(COMMUNICATION_ACTIVITY_END_AVAILABILITY_TIME)
    var activityEndAvailabilityTime: LocalTime? = null,
): GenericProjectEntity()
