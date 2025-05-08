package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationFields.COMMUNICATION_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationQueries.COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationQueries.COMMUNICATION_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationQueries.MOVEMENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationQueries.SELECT_COMMUNICATION_SEARCH
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.activity.CommunicationQueries.SELECT_LINKED_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.PROJECT_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.PROJECT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LINKED_PROJECT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface ICommunicationEntityRepository: ReactiveCrudRepository<CommunicationEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_COMMUNICATION_SEARCH, $SELECT_LINKED_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $COMMUNICATION_TABLE t $MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $COMMUNICATION_TEXT_SEARCH_CLAUSE AND $COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$COMMUNICATION_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(
        projectId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<CommunicationEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $COMMUNICATION_TABLE t
        WHERE $PROJECT_CLAUSE AND $VISIBLE_CLAUSE AND $COMMUNICATION_TEXT_SEARCH_CLAUSE AND $COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
        """
    )
    fun countAll(
        projectId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_COMMUNICATION_SEARCH, $SELECT_LINKED_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $COMMUNICATION_TABLE t $MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$COMMUNICATION_MOVEMENT_ID = :movementId AND $VISIBLE_CLAUSE AND $COMMUNICATION_TEXT_SEARCH_CLAUSE AND $COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
        ORDER BY similarity_score DESC, t.$COMMUNICATION_DATE_TIME DESC
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAllByMovementId(
        projectId: UUID,
        movementId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
        limit: Int,
        offset: Int,
    ): Flux<CommunicationEntity>

    @Query(
        """
        SELECT COUNT(t.$ID)
        FROM $COMMUNICATION_TABLE t
        WHERE $PROJECT_CLAUSE AND t.$COMMUNICATION_MOVEMENT_ID = :movementId AND $VISIBLE_CLAUSE AND $COMMUNICATION_TEXT_SEARCH_CLAUSE AND $COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
        """
    )
    fun countAllByMovementId(
        projectId: UUID,
        movementId: UUID,
        textSearched: String?,
        visibilitySearched: Boolean?,
        startDateTimeSearched: ZonedDateTime?,
        endDateTimeSearched: ZonedDateTime?,
    ): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_LINKED_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $COMMUNICATION_TABLE t $MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID IN (:ids) AND $VISIBLE_CLAUSE
        """
    )
    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<CommunicationEntity>


    @Query(
        """
        SELECT t.*, $SELECT_LINKED_MOVEMENT, $SELECT_LINKED_PROJECT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $COMMUNICATION_TABLE t $MOVEMENT_JOIN $PROJECT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $PROJECT_CLAUSE AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<CommunicationEntity>
}
