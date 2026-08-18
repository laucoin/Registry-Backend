package fr.laucoin.registry.backend.domain.handler

import org.springframework.http.CacheControl
import org.springframework.security.web.server.header.XContentTypeOptionsServerHttpHeadersWriter.NOSNIFF
import org.springframework.security.web.server.header.XContentTypeOptionsServerHttpHeadersWriter.X_CONTENT_OPTIONS
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * Transport headers for the (private) JSON API: `nosniff` on
 * everything and `Cache-Control: no-store` as the default for authenticated
 * responses. Applied at commit time so the deliberate exceptions (the metadata
 * revalidation headers) win when already set. HSTS/CSP are public-tier
 * (Nuxt) concerns.
 */
@Component
class ApiHeadersHandler : WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		exchange.response.beforeCommit {
			val headers = exchange.response.headers
			headers.set(X_CONTENT_OPTIONS, NOSNIFF)
			if (headers.cacheControl == null) {
				headers.setCacheControl(CacheControl.noStore())
			}
			Mono.empty()
		}
		return chain.filter(exchange)
	}
}
