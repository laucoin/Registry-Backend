package fr.laucoin.registry.backend.domain.handler

import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Forces the CSRF token to be resolved so its cookie is actually sent.
 *
 * In WebFlux the token is exposed as a `Mono` on an exchange attribute and is only computed when
 * something subscribes to it. Nothing does on a request that passes CSRF, so without this filter the
 * `XSRF-TOKEN` cookie is never written and the frontend has no token to echo back — the first
 * mutating call then fails, and nothing in the logs explains why.
 */
@Component
class CsrfTokenHandler: WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val token = exchange.getAttribute<Mono<CsrfToken>>(CsrfToken::class.java.name)
			?: return chain.filter(exchange)

		return token.then(chain.filter(exchange))
	}
}
