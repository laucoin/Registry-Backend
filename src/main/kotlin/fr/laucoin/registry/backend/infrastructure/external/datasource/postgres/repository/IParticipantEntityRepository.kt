package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.GROUPS_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.GROUP_BY_PARTICIPANT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.GROUP_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.IN_DATE_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.NOT_PURGED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.PRESENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.USER_JOIN
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
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $EVENT_JOIN $GROUPS_JOIN $GROUP_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE AND $PRESENT_CLAUSE AND $IN_DATE_RANGE_CLAUSE
        GROUP BY $GROUP_BY_PARTICIPANT
        """
    )
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<ParticipantEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $EVENT_JOIN $GROUPS_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE AND t.$ID IN (:ids)
        GROUP BY $GROUP_BY_PARTICIPANT
        """
    )
    fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<ParticipantEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $EVENT_JOIN $GROUPS_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE AND t.$ID = :id
        GROUP BY $GROUP_BY_PARTICIPANT
        """
    )
    fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<ParticipantEntity>
}
