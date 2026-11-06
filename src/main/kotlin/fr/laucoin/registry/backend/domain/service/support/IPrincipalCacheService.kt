package fr.laucoin.registry.backend.domain.service.support

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat du cache des utilisateurs courants par identifiant OIDC.
 * Périmètre : Déclare la lecture avec chargement, l'invalidation unitaire et l'invalidation groupée.
 * Limites : Contrat uniquement ; la durée de vie et la taille viennent de la configuration.
 */
interface IPrincipalCacheService {
	fun get(oidcId: UUID, loader: () -> Mono<CurrentUserModel>): Mono<CurrentUserModel>
	fun invalidate(oidcId: UUID?)
	fun invalidateAll(oidcIds: Collection<UUID>)
}
