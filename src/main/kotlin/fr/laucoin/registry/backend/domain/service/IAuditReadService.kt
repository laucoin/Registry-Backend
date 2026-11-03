package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import java.util.UUID

/**
 * Objectif : Contrat de cas d'usage de la trace d'audit des lectures de données personnelles.
 * Périmètre : Enregistre une lecture, avec ou sans ressource ciblée, sans attendre son écriture.
 * Limites : Contrat uniquement ; la déduplication des lectures de liste appartient à l'implémentation.
 */
interface IAuditReadService {
	fun recordRead(
		currentUser: CurrentUserModel,
		resourceType: AuditResourceTypeEnum,
		resourceId: UUID?,
		projectId: UUID?,
		client: ClientContextModel,
		outcome: AuditOutcomeEnum = AuditOutcomeEnum.SUCCESS,
	)
}
