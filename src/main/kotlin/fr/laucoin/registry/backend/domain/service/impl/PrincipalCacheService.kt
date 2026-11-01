package fr.laucoin.registry.backend.domain.service.impl

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.UUID

/**
 * Objectif : Met en cache les principals résolus à partir d'un JWT.
 * Périmètre : Porte le cache asynchrone borné en taille et en durée et son invalidation par identifiant OIDC.
 * Limites : Cache local à l'instance ; une modification de droits n'est visible qu'après invalidation ou expiration.
 */
@Service
class PrincipalCacheService(
	@Value($$"${registry.security.oauth2.cache.principal.ttl-seconds}")
	ttlSeconds: Long,
) : IPrincipalCacheService {
	private val cache: AsyncCache<UUID, CurrentUserModel> = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofSeconds(ttlSeconds))
		.maximumSize(MAX_CACHED_PRINCIPALS)
		.buildAsync()

	override fun get(oidcId: UUID, loader: () -> Mono<CurrentUserModel>): Mono<CurrentUserModel> {
		return Mono.fromFuture { cache.get(oidcId) { _, _ -> loader().orFailWhenEmpty().toFuture().thenApply { checkNotNull(it) } } }
	}

	private fun Mono<CurrentUserModel>.orFailWhenEmpty(): Mono<CurrentUserModel> {
		return switchIfEmpty(Mono.error(IllegalStateException("Principal resolution completed without a value")))
	}

	override fun invalidate(oidcId: UUID?) {
		if (oidcId != null) cache.synchronous().invalidate(oidcId)
	}

	override fun invalidateAll(oidcIds: Collection<UUID>) {
		if (oidcIds.isNotEmpty()) cache.synchronous().invalidateAll(oidcIds)
	}

	private companion object {
		const val MAX_CACHED_PRINCIPALS = 10_000L
	}
}
