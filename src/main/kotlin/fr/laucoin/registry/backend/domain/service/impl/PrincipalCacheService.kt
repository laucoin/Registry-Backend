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
import java.util.concurrent.CompletableFuture

@Service
class PrincipalCacheService(
	@Value($$"${registry.security.oauth2.cache.principal.ttl-seconds}")
	ttlSeconds: Long,
) : IPrincipalCacheService {
	private val cache: AsyncCache<UUID, CurrentUserModel> = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofSeconds(ttlSeconds))
		.buildAsync()

	override fun get(oidcId: UUID, loader: () -> Mono<CurrentUserModel>): Mono<CurrentUserModel> {
		return Mono.fromFuture { cache.get(oidcId) { _, _ -> loader().toCompletableFuture() } }
	}

	private fun Mono<CurrentUserModel>.toCompletableFuture(): CompletableFuture<CurrentUserModel> {
		val future = CompletableFuture<CurrentUserModel>()
		subscribe(future::complete, future::completeExceptionally) {
			future.completeExceptionally(IllegalStateException("Principal resolution completed without a value"))
		}
		return future
	}

	override fun invalidate(oidcId: UUID?) {
		if (oidcId != null) cache.synchronous().invalidate(oidcId)
	}

	override fun invalidateAll(oidcIds: Collection<UUID>) {
		if (oidcIds.isNotEmpty()) cache.synchronous().invalidateAll(oidcIds)
	}
}
