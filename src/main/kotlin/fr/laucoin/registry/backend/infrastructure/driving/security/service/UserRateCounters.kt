package fr.laucoin.registry.backend.infrastructure.driving.security.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import java.time.Clock
import java.time.Duration
import java.util.UUID

/**
 * Objectif : Tient un compteur de fenêtre par utilisateur authentifié pour les limiteurs de débit.
 * Périmètre : Porte le cache borné des compteurs (expiration après une période de grâce) et décide si un appel reste dans la capacité.
 * Limites : En mémoire et par instance ; ne répond rien au client, le limiteur appelant s'en charge.
 */
class UserRateCounters(
	private val clock: Clock,
	private val capacity: Int,
	private val window: Duration,
	maxTrackedUsers: Long,
) {
	private val counters: Cache<UUID, SlidingWindowCounter> = Caffeine.newBuilder()
		.expireAfterAccess(window.multipliedBy(EXPIRY_GRACE_MULTIPLIER))
		.maximumSize(maxTrackedUsers)
		.build()

	fun tryConsume(userId: UUID): Boolean = counters.get(userId) { SlidingWindowCounter(clock, capacity, window) }.tryConsume()

	private companion object {
		const val EXPIRY_GRACE_MULTIPLIER = 10L
	}
}
