package fr.laucoin.registry.backend.domain.service

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * ADR 018 — the reactive-safe caching reference instance: a Caffeine
 * [AsyncCache] behind a [Mono] API, so cache access composes with the reactive
 * pipeline and never blocks the event loop (Spring's blocking `@Cacheable` is
 * deliberately not used, per the ADR).
 *
 * Entries expire after [ttl] (per-replica freshness bound) and writers evict
 * explicitly via [evictAll]/[evict]. A failed load is never cached.
 */
class ReactiveCacheService<K : Any, V : Any>(
	ttl: Duration,
	maximumSize: Long = 1_000,
) {
	private val cache: AsyncCache<K, V> = Caffeine.newBuilder()
		.expireAfterWrite(ttl)
		.maximumSize(maximumSize)
		.buildAsync()

	/**
	 * `single()` turns an empty load into an error — "no value" is never
	 * cached; `thenApply` restores the non-null type. Cancellation is
	 * suppressed (`suppressCancel`) because the future is the shared
	 * [AsyncCache] entry: one subscriber disconnecting mid-load must not
	 * cancel the load for every concurrent caller.
	 */
	fun get(key: K, loader: (K) -> Mono<V>): Mono<V> {
		return Mono.fromFuture(
			{
				cache.get(key) { cacheKey, _ ->
					loader(cacheKey).single().toFuture().thenApply { it!! }
				}
			},
			true,
		)
	}

	fun evict(key: K) {
		cache.synchronous().invalidate(key)
	}

	fun evictAll() {
		cache.synchronous().invalidateAll()
	}
}
