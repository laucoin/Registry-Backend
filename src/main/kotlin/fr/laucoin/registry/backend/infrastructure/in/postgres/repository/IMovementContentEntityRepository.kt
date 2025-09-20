package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository

import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericFields.LINKED_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.CONTENT_JOIN
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.SELECT_CONTENT
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementQueries.WITH_CURRENT_MOVEMENT
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
        WHERE mt.$LINKED_PROJECT_ID = :projectId AND t.$MOVEMENT_CONTENT_MOVEMENT_ID IN (:movementIds)
    """
	)
	fun findAllByMovementIds(projectId: UUID, movementIds: List<UUID>): Flux<MovementContentEntity>

	@Query(
		"""
        WITH $WITH_CURRENT_MOVEMENT
        SELECT t.*, $SELECT_CONTENT
        FROM current_movement
        INNER JOIN $MOVEMENT_CONTENT_TABLE t ON t.movement_id = current_movement.id AND t.participant_id = current_movement.participant_id
        INNER JOIN $MOVEMENT_TABLE mt ON t.$MOVEMENT_CONTENT_MOVEMENT_ID = mt.$ID $CONTENT_JOIN
        WHERE tb_participant.$LINKED_PROJECT_ID = :projectId AND t.$MOVEMENT_CONTENT_MOVEMENT_ID IN (:movementIds)
    """
	)
	fun findCurrentByMovementIds(projectId: UUID, movementIds: List<UUID>): Flux<MovementContentEntity>
}
