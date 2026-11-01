package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.AuditModel

/**
 * Objectif : Contrat de cas d'usage de la trace d'audit des accès aux données personnelles.
 * Périmètre : Enregistre un événement d'audit sans attendre son écriture.
 * Limites : Contrat uniquement ; l'implémentation porte la file d'attente et sa politique de débordement.
 */
interface IAuditService {
	fun record(audit: AuditModel)
}
