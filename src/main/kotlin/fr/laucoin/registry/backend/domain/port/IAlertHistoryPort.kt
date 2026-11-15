package fr.laucoin.registry.backend.domain.port

import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès à l'historique du alerte.
 * Périmètre : Archive l'état courant du alerte avant sa modification.
 * Limites : Écriture seule ; l'appelant doit détenir le verrou de ligne et ouvrir la transaction.
 */
interface IAlertHistoryPort {
	fun archiveAlert(id: UUID): Mono<Unit>
}
