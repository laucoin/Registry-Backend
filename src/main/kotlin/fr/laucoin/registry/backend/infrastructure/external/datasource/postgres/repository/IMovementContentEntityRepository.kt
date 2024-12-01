package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementContentEntity
import java.util.UUID
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface IMovementContentEntityRepository: ReactiveCrudRepository<MovementContentEntity, UUID>
