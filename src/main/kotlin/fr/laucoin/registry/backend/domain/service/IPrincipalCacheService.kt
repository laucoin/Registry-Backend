package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IPrincipalCacheService {
	fun get(oidcId: UUID, loader: () -> Mono<CurrentUserModel>): Mono<CurrentUserModel>
	fun invalidate(oidcId: UUID?)
	fun invalidateAll(oidcIds: Collection<UUID>)
}
