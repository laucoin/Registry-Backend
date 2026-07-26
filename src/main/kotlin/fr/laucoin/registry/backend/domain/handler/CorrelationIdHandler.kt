package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_CONTEXT_KEY
import fr.laucoin.registry.backend.domain.constant.AuditConst.CORRELATION_ID_HEADER
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * ADR 019 §5 (and ADR 020) — one correlation id per request: taken from the
 * caller (the Nuxt BFF forwards its own) when well-formed, generated
 * otherwise; echoed on the response and pushed into the Reactor context so the
 * audit trail can stamp every entry. Highest precedence so even requests
 * rejected by the security chain carry the id.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdHandler : WebFilter {
	private companion object {
		private val WELL_FORMED = Regex("^[A-Za-z0-9-]{8,64}$")
	}

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val correlationId = exchange.request.headers.getFirst(CORRELATION_ID_HEADER)
			?.takeIf { WELL_FORMED.matches(it) }
			?: UUID.randomUUID().toString()

		exchange.response.headers.set(CORRELATION_ID_HEADER, correlationId)
		return chain.filter(exchange)
			.contextWrite { it.put(CORRELATION_ID_CONTEXT_KEY, correlationId) }
	}
}
