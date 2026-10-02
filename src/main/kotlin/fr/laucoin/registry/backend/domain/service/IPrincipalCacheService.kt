package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Caches the resolved [CurrentUserModel] per OIDC ID so repeated requests from the same principal
 * don't re-hit the database on every call. Callers supply a loader for cache misses and must
 * invalidate an entry (or several) whenever the underlying User/authorities change.
 */
interface IPrincipalCacheService {
	fun get(oidcId: UUID, loader: () -> Mono<CurrentUserModel>): Mono<CurrentUserModel>
	fun invalidate(oidcId: UUID?)
	fun invalidateAll(oidcIds: Collection<UUID>)
}
