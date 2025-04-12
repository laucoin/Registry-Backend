package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityFields.ACTIVITY_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityQueries.ACTIVITY_AVAILABILITY_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityQueries.ACTIVITY_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.ActivityQueries.DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
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
interface IActivityEntityRepository: ReactiveCrudRepository<ActivityEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
        ORDER BY t.$ACTIVITY_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<ActivityEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $ACTIVITY_TABLE t
        WHERE $EVENT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
    )
    fun findAllByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ACTIVITY_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $ACTIVITY_AVAILABILITY_CLAUSE AND $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
        ORDER BY t.$ACTIVITY_NAME
        LIMIT :limit
        """
    )
    fun findWithLimit(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        availabilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
    ): Flux<ActivityEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $ACTIVITY_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityEntity>
}
