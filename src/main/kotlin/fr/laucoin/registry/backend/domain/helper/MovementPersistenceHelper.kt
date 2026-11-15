package fr.laucoin.registry.backend.domain.helper

import org.springframework.stereotype.Component
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.MOVEMENT
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.port.IMovementHistoryPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Regroupe les accès de persistance communs aux services de mouvement.
 * Périmètre : Lecture d'un mouvement existant, archivage de son état courant et verrou de ligne.
 * Limites : Ne porte aucune règle métier.
 */
@Component
class MovementPersistenceHelper(
	private val port: IMovementPort,
	private val historyPort: IMovementHistoryPort,
	private val rowLockPort: IRowLockPort,
) {
	fun findMovement(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return port.findById(projectId, id, isVisible).notFoundIfEmpty(id)
	}

	fun archive(movement: MovementModel): Mono<MovementModel> {
		return historyPort.archiveMovement(movement.id!!).thenReturn(movement)
	}

	fun lock(movementId: UUID): Mono<Unit> = rowLockPort.lock(MOVEMENT, movementId)
}
