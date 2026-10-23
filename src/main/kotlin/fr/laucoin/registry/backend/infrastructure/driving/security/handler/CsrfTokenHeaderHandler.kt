package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService.Companion.HEADER_NAME
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Purpose: Exposes the CSRF token in a response header.
 * Scope: Sets the header on every response, since nothing subscribes to the lazy token of a JSON API.
 * Limits: The token is a cheap deterministic value; it stores nothing.
 */
class CsrfTokenHeaderHandler(private val tokenService: CsrfTokenService): WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		exchange.response.headers.set(HEADER_NAME, tokenService.computeToken(exchange))
		return chain.filter(exchange)
	}
}
