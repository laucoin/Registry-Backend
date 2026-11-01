package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AuditModel
import java.time.LocalDate
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'écriture de la trace d'audit des accès aux données personnelles.
 * Périmètre : Persiste un événement d'audit et supprime ceux antérieurs à un seuil de rétention.
 * Limites : Ne modifie aucun événement et n'expose aucune lecture, hormis le décompte des événements à purger.
 */
interface IAuditPort {
	fun record(audit: AuditModel): Mono<Unit>

	fun purgeOlderThan(deleteOlderThan: LocalDate, dryRun: Boolean): Mono<Long>
}
