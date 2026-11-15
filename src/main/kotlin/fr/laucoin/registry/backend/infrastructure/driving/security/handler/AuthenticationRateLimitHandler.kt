package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.SlidingWindowCounter
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TooManyRequestsResponder
import java.time.Clock
import java.time.Duration
import org.springframework.http.HttpMethod.POST
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Objectif : Limite le débit des endpoints de token non authentifiés.
 * Périmètre : Compte les appels POST par adresse client réelle (résolue via les proxies de confiance) et chemin sur une fenêtre glissante et répond 429 au-delà de la capacité.
 * Limites : En mémoire et par instance ; le nombre de clients suivis est borné.
 */
class AuthenticationRateLimitHandler(
	private val clientContextService: ClientContextService,
	private val responder: TooManyRequestsResponder,
	private val clock: Clock,
	private val capacity: Int,
	private val windowSeconds: Long,
	maxTrackedClients: Long,
) : WebFilter {
	private val pathMatcher = AntPathMatcher()

	private val counters: Cache<String, SlidingWindowCounter> = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofSeconds(windowSeconds * EXPIRY_GRACE_MULTIPLIER))
		.maximumSize(maxTrackedClients)
		.build()

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		if (request.method != POST) return chain.filter(exchange)

		val matchedPath = RATE_LIMITED_PATHS.firstOrNull { pathMatcher.match(it, request.path.value()) }
			?: return chain.filter(exchange)

		val clientId = clientContextService.resolve(request).ip ?: return chain.filter(exchange)
		val allowed = counters.get("$clientId|$matchedPath") {
			SlidingWindowCounter(clock, capacity, Duration.ofSeconds(windowSeconds))
		}.tryConsume()

		return if (allowed) chain.filter(exchange) else responder.respond(exchange, windowSeconds)
	}

	private companion object {
		val RATE_LIMITED_PATHS = listOf("/api/*/authentication/token", "/api/*/authentication/token/refresh")
		const val EXPIRY_GRACE_MULTIPLIER = 10L
	}
}
