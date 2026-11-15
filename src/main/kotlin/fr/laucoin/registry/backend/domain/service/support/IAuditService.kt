package fr.laucoin.registry.backend.domain.service.support

import fr.laucoin.registry.backend.domain.model.AuditModel
import java.time.LocalDate
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage de la trace d'audit des accès aux données personnelles.
 * Périmètre : Enregistre un événement d'audit sans attendre son écriture et purge ceux dépassant la rétention.
 * Limites : Contrat uniquement ; l'implémentation porte la file d'attente et sa politique de débordement.
 */
interface IAuditService {
	fun record(audit: AuditModel)

	fun purgeAuditsIfNecessary(deleteOlderThan: LocalDate, dryRun: Boolean): Mono<Long>
}
