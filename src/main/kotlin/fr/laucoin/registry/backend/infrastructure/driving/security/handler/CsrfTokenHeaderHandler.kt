package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Objectif : Expose le token CSRF dans un en-tête de réponse.
 * Périmètre : Positionne l'en-tête sur chaque réponse, car rien ne s'abonne au token paresseux d'une API JSON.
 * Limites : Le token est une valeur déterministe peu coûteuse ; il ne stocke rien.
 */
class CsrfTokenHeaderHandler(private val tokenService: CsrfTokenService): WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		exchange.response.headers.set(HEADER_NAME, tokenService.computeToken(exchange))
		return chain.filter(exchange)
	}
}
