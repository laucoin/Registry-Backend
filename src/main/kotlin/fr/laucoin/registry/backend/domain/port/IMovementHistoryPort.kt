package fr.laucoin.registry.backend.domain.port

import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès à l'historique du mouvement.
 * Périmètre : Archive l'état courant du mouvement avant sa modification.
 * Limites : Écriture seule ; l'appelant doit détenir le verrou de ligne et ouvrir la transaction.
 */
interface IMovementHistoryPort {
	fun archiveMovement(id: UUID): Mono<Unit>
}
