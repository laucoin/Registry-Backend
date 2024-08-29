package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantQueries.IN_DATE_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantQueries.NOT_PURGED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantQueries.USER_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.ONLY_VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_EVENT
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IParticipantEntityRepository: ReactiveCrudRepository<ParticipantEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE AND $IN_DATE_RANGE_CLAUSE
        """
    )
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<ParticipantEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE AND t.$ID = :id
        """
    )
    fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantEntity>
}
