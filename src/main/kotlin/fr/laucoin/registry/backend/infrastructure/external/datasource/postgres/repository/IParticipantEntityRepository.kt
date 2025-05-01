package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_GROUP_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantFields.PARTICIPANT_END_AVAILABILITY_TIME
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
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TYPE_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_LINKED_USER
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.SELECT_PARTICIPANT_SEARCH
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.USER_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_GROUPS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantQueries.WITH_PARTICIPANT_LAST_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDate
import java.time.LocalTime
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
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        projectId: UUID,
        textSearched: String?,
        typeSearched: ParticipantTypeEnum?,
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
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        projectId: UUID,
        textSearched: String?,
        typeSearched: ParticipantTypeEnum?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $GROUP_CONTENT_TABLE ON t.id = $GROUP_CONTENT_TABLE.$GROUP_CONTENT_PARTICIPANT_ID AND $GROUP_CONTENT_TABLE.$GROUP_CONTENT_GROUP_ID = :groupId
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllByGroupId(
        projectId: UUID,
        groupId: UUID,
        textSearched: String?,
        typeSearched: ParticipantTypeEnum?,
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
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        """
    )
    fun countAllByGroupId(
        projectId: UUID,
        groupId: UUID,
        textSearched: String?,
        typeSearched: ParticipantTypeEnum?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
    )
    fun findAllByIds(
        projectId: UUID,
        ids: List<UUID>,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?
    ): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND t.$PARTICIPANT_USER_ID = :userId
        """
    )
    fun findByUserId(projectId: UUID, userId: UUID, dateTimeSearched: ZonedDateTime?): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_PARTICIPANT_SEARCH, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND $PARTICIPANT_TEXT_SEARCH_CLAUSE AND $PARTICIPANT_TYPE_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $PARTICIPANT_AVAILABILITY_CLAUSE AND $PARTICIPANT_PRESENCE_CLAUSE AND $DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$PARTICIPANT_LAST_NAME
        LIMIT :limit
        """
    )
    fun findWithLimit(
        projectId: UUID,
        textSearched: String?,
        typeSearched: ParticipantTypeEnum?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
    ): Flux<ParticipantEntity>

    @Query(
        """
        WITH $WITH_PARTICIPANT_LAST_MOVEMENT, $WITH_PARTICIPANT_GROUPS
        SELECT t.*, $SELECT_LINKED_USER, $SELECT_LAST_MOVEMENT, $SELECT_LINKED_GROUPS, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $PARTICIPANT_TABLE t $USER_JOIN $LAST_MOVEMENT_JOIN $GROUPS_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?, dateTimeSearched: ZonedDateTime?): Mono<ParticipantEntity>

    @Query(
        """
        UPDATE $PARTICIPANT_TABLE SET $PARTICIPANT_END_AVAILABILITY_TIME = :endAvailabilityTime, $PARTICIPANT_END_AVAILABILITY_DATE = :endAvailabilityDate
        WHERE $ID IN (:ids)
        RETURNING *
        """
    )
    fun updateAllEndAvailability(
        ids: List<UUID>,
        endAvailabilityTime: LocalTime?,
        endAvailabilityDate: LocalDate?
    ): Flux<ParticipantEntity>
}
