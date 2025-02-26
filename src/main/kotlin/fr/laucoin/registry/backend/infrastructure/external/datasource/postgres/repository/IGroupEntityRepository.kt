package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.DATE_IN_GROUP_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.GROUP_BY_GROUP
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.GROUP_PRESENCE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.GROUP_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.SELECT_CONTENT
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
interface IGroupEntityRepository: ReactiveCrudRepository<GroupEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
        ORDER BY t.$GROUP_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<GroupEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $GROUP_TABLE t
        WHERE $EVENT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
    )
    fun findAllByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupEntity>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $GROUP_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $GROUP_PRESENCE_CLAUSE AND $DATE_IN_GROUP_DATES_RANGE_CLAUSE
        ORDER BY t.$GROUP_NAME
        LIMIT :limit
        """
    )
    fun findWithLimit(
        eventId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        presenceSearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
        limit: Int,
    ): Flux<GroupEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CONTENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $CONTENT_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        GROUP BY $GROUP_BY_GROUP
        """
    )
    fun findById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<GroupEntity>
}
