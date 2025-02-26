package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_USER_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.GROUPS_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.LAST_MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.NOT_PURGED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.PARTICIPANT_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.PARTICIPANT_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.USER_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.EVENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_EVENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
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
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY t.$PARTICIPANT_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT COUNT(t.$ID)
        FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN $GROUPS_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $GROUP_CONTENT_TABLE ON t.id = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID AND $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = :groupId
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY t.$PARTICIPANT_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllByGroupId(
        eventId: UUID,
        groupId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT COUNT(t.$ID)
        FROM $PARTICIPANT_TABLE t $LAST_MOVEMENT_JOIN $GROUPS_JOIN
        INNER JOIN $GROUP_CONTENT_TABLE ON t.id = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID AND $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = :groupId
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        """
    )
    fun countAllByGroupId(
        eventId: UUID,
        groupId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
    )
    fun findAllByIds(
        eventId: UUID,
        ids: List<UUID>,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?
    ): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND t.$PARTICIPANT_USER_ID = :userId
        """
    )
    fun findByUserId(eventId: UUID, userId: UUID, dateTimeSearched: ZonedDateTime?): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY t.$PARTICIPANT_LAST_NAME
        LIMIT :limit
        """
    )
    fun findWithLimit(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
    ): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $EVENT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(eventId: UUID, id: UUID, visibilitySearched: Boolean?, dateTimeSearched: ZonedDateTime?): Mono<ParticipantEntity>
}
