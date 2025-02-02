package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.GROUP_BY_MOVEMENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.IN_DATE_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.SELECT_CONTENT
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
interface IMovementEntityRepository: ReactiveCrudRepository<MovementEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CONTENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $CONTENT_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND $IN_DATE_RANGE_CLAUSE AND (:type IS NULL OR t.$MOVEMENT_TYPE = :type)
        GROUP BY $GROUP_BY_MOVEMENT
        """
    )
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CONTENT, $SELECT_LINKED_EVENT, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $MOVEMENT_TABLE t $CONTENT_JOIN $EVENT_JOIN $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $EVENT_CLAUSE AND $ONLY_VISIBLE_CLAUSE AND t.$ID = :id
        GROUP BY $GROUP_BY_MOVEMENT
        """
    )
    fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<MovementEntity>
}
