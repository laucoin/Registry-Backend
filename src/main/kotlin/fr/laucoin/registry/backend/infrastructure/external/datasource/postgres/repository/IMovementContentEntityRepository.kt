package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.LINKED_EVENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementQueries.SELECT_CONTENT
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface IMovementContentEntityRepository: ReactiveCrudRepository<MovementContentEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_CONTENT
        FROM $MOVEMENT_CONTENT_TABLE t
        INNER JOIN $MOVEMENT_TABLE mt ON t.$MOVEMENT_CONTENT_MOVEMENT_ID = mt.$ID $CONTENT_JOIN
        WHERE mt.$LINKED_EVENT_ID = :eventId AND t.$MOVEMENT_CONTENT_MOVEMENT_ID IN (:movementIds)
    """
    )
    fun findAllByMovementIds(eventId: UUID, movementIds: List<UUID>): Flux<MovementContentEntity>
}
