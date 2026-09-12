package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.service.impl.CsrfTokenService
import fr.laucoin.registry.backend.domain.service.impl.CsrfTokenService.Companion.HEADER_NAME
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Spring's CsrfWebFilter only exposes the CsrfToken it generated/loaded as a lazily-subscribed
 * exchange attribute (`Mono<CsrfToken>`) — nothing ever subscribes to it for a pure JSON API (no
 * Thymeleaf form referencing `_csrf.token` to trigger that), so the repository's `saveToken` would
 * never actually run. This filter sets the response header directly instead, independent of that
 * lazy-subscription mechanism — safe here because the token is a cheap, deterministic computation,
 * not something worth deferring.
 */
class CsrfTokenHeaderHandler(private val tokenService: CsrfTokenService): WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		exchange.response.headers.set(HEADER_NAME, tokenService.computeToken(exchange))
		return chain.filter(exchange)
	}
}
