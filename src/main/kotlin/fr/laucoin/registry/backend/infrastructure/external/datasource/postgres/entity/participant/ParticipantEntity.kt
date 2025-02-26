package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_AVAILABLE_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(PARTICIPANT_TABLE)
data class ParticipantEntity(
    @Column(PARTICIPANT_FIRST_NAME)
    var firstName: String? = null,
    @Column(PARTICIPANT_LAST_NAME)
    var lastName: String? = null,
    @Column(PARTICIPANT_BIRTHDAY)
    var birthday: LocalDate? = null,
    @ReadOnlyProperty
    @Column(PARTICIPANT_GROUPS)
    var groups: String? = "[]",
    @ReadOnlyProperty
    @Column(PARTICIPANT_AVAILABLE_GROUPS)
    var availableGroups: String? = "[]",
    @ReadOnlyProperty
    @Column(PARTICIPANT_LAST_MOVEMENT_TYPE)
    var lastMovementType: MovementTypeEnum? = null,
    @ReadOnlyProperty
    @Column(PARTICIPANT_LAST_MOVEMENT_DATE_TIME)
    var lastMovementDateTime: ZonedDateTime? = null,
    @Column(PARTICIPANT_START_AVAILABILITY_DATE)
    var startAvailabilityDate: LocalDate? = null,
    @Column(PARTICIPANT_START_AVAILABILITY_TIME)
    var startAvailabilityTime: LocalTime? = null,
    @Column(PARTICIPANT_END_AVAILABILITY_DATE)
    var endAvailabilityDate: LocalDate? = null,
    @Column(PARTICIPANT_END_AVAILABILITY_TIME)
    var endAvailabilityTime: LocalTime? = null,
    @Column(PARTICIPANT_USER_ID)
    var userId: UUID? = null,
    @ReadOnlyProperty
    @Column(PARTICIPANT_USER_FIRST_NAME)
    var userFirstName: String? = null,
    @ReadOnlyProperty
    @Column(PARTICIPANT_USER_LAST_NAME)
    var userLastName: String? = null,
    @ReadOnlyProperty
    @Column(PARTICIPANT_USER_EMAIL)
    var userEmail: String? = null,
    @Column(PARTICIPANT_PURGED)
    var purged: Boolean? = null,
): GenericEventEntity()
