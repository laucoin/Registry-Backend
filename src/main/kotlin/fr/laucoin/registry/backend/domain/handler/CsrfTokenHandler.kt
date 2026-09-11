package fr.laucoin.registry.backend.domain.handler

import org.springframework.security.web.server.csrf.CsrfToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class CsrfTokenHandler : WebFilter {

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val token = exchange.getAttribute<Mono<CsrfToken>>(CsrfToken::class.java.name)
			?: return chain.filter(exchange)

		return token.then(chain.filter(exchange))
	}
}
