package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_PURGED
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import java.time.LocalDate
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
    @Column(PARTICIPANT_BEGIN)
    var begin: ZonedDateTime? = null,
    @Column(PARTICIPANT_END)
    var end: ZonedDateTime? = null,
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
