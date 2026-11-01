package fr.laucoin.registry.backend.domain.port

import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'accès à l'historique du communication.
 * Périmètre : Archive l'état courant du communication avant sa modification.
 * Limites : Écriture seule ; l'appelant doit détenir le verrou de ligne et ouvrir la transaction.
 */
interface ICommunicationHistoryPort {
	fun archiveCommunication(id: UUID): Mono<Unit>
}
