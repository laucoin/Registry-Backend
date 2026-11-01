package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AuditModel
import reactor.core.publisher.Mono

/**
 * Objectif : Port driven donnant au domaine l'écriture de la trace d'audit des accès aux données personnelles.
 * Périmètre : Persiste un événement d'audit.
 * Limites : Écriture seule ; ne lit, ne modifie et ne supprime aucun événement.
 */
interface IAuditPort {
	fun record(audit: AuditModel): Mono<Unit>
}
