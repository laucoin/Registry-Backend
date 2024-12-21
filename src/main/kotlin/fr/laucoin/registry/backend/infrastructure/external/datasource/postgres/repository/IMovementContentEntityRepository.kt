package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface IMovementContentEntityRepository: ReactiveCrudRepository<MovementContentEntity, UUID> {
    @Query("DELETE FROM $MOVEMENT_CONTENT_TABLE WHERE $MOVEMENT_CONTENT_MOVEMENT_ID = :movementId AND $MOVEMENT_CONTENT_PARTICIPANT_ID IN (:participantIds::uuid[])")
    fun deleteAllByMovementIdAndParticipantId(movementId: UUID, participantIds: List<UUID>): Mono<Void>
}
