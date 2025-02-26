package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupFields.GROUP_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.GROUP_BY_GROUP
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.IN_DATE_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.PRESENT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupQueries.SELECT_CONTENT
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
interface IGroupEntityRepository: ReactiveCrudRepository<GroupEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CONTENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $CONTENT_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $PRESENT_CLAUSE AND $IN_DATE_RANGE_CLAUSE
        GROUP BY $GROUP_BY_GROUP
        """
    )
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<GroupEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CONTENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $CONTENT_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND t.$ID IN (:ids)
        GROUP BY $GROUP_BY_GROUP
        """
    )
    fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<GroupEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CONTENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $GROUP_TABLE t $CONTENT_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND t.$ID = :id
        GROUP BY $GROUP_BY_GROUP
        """
    )
    fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<GroupEntity>
}
