package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TooManyRequestsResponder
import fr.laucoin.registry.backend.infrastructure.driving.security.service.UserRateCounters
import java.time.Clock
import java.time.Duration
import java.util.Optional
import java.util.UUID
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Objectif : Limite le débit de la recherche d'utilisateurs à inviter pour empêcher l'extraction de l'annuaire de la plateforme.
 * Périmètre : Compte les appels GET et HEAD de recherche d'utilisateurs par utilisateur authentifié, tous projets confondus, sur une fenêtre de temps ; répond 429 au-delà de la capacité.
 * Limites : En mémoire et par instance ; le nombre d'utilisateurs suivis est borné. Doit s'exécuter après l'authentification ; les requêtes non authentifiées lui échappent et sont refusées en aval.
 */
class UserSearchRateLimitHandler(
	private val responder: TooManyRequestsResponder,
	clock: Clock,
	capacity: Int,
	private val windowSeconds: Long,
	maxTrackedUsers: Long,
) : WebFilter {
	private val pathMatcher = AntPathMatcher()

	private val counters = UserRateCounters(clock, capacity, Duration.ofSeconds(windowSeconds), maxTrackedUsers)

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		if (request.method !in SEARCH_METHODS || !pathMatcher.match(USER_SEARCH, request.path.value())) return chain.filter(exchange)

		return ReactiveSecurityContextHolder.getContext()
			.mapNotNull { (it.authentication?.principal as? CurrentUserModel)?.id }
			.map { Optional.of(it) }
			.defaultIfEmpty(Optional.empty())
			.flatMap { limit(it.orElse(null), exchange, chain) }
	}

	private fun limit(userId: UUID?, exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		if (userId == null || counters.tryConsume(userId)) return chain.filter(exchange)
		return responder.respond(exchange, windowSeconds)
	}

	private companion object {
		const val USER_SEARCH = "/api/*/projects/*/profiles/search/users"
		val SEARCH_METHODS = setOf(GET, HEAD)
	}
}
