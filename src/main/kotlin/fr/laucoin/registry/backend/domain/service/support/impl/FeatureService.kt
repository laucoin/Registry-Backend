package fr.laucoin.registry.backend.domain.service.support.impl

import fr.laucoin.registry.backend.domain.service.support.IFeatureService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Objectif : Fournit l'état des fonctionnalités activables par configuration.
 * Périmètre : Lit les indicateurs `registry.feature.*` liés aux fonctionnalités métier.
 * Limites : Ne porte aucune règle métier ; chaque appelant applique la conséquence d'un indicateur.
 */
@Service
class FeatureService(
	@param:Value($$"${registry.feature.profile.invite-by-email.enabled}")
	private val inviteByEmailEnabled: Boolean,
): IFeatureService {
	override fun isInviteByEmailEnabled(): Boolean {
		return inviteByEmailEnabled
	}
}
