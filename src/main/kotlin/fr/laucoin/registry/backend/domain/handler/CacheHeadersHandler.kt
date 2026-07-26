package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.annotation.HttpCacheable
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpStatus.NOT_MODIFIED
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.util.UUID

/**
 * ADR 018 §4 — HTTP caching for the cacheable reference GETs (metadata). Their
 * payloads are code-derived (enums + bundled translations), so they only change
 * with a deployment: the ETag is deterministic per (deployment, path, locale)
 * and a matching `If-None-Match` short-circuits to `304 Not Modified` without
 * re-rendering the body. `Cache-Control: private` lets the Nuxt BFF and
 * browsers reuse the body for [maxAgeSeconds] before revalidating;
 * `Vary: Accept-Language` keeps locale variants apart, appended so the CORS
 * processor's own Vary values survive. The headers are written at commit time
 * and never on an error response (a still-unset status at commit means the
 * implied 200; error handlers always set an explicit 4xx/5xx) — an error must
 * never carry a validator, or revalidation would pin it for the whole max-age.
 *
 * Applies exclusively to endpoints whose contract method declares
 * [HttpCacheable] — the behaviour is visible on the endpoint, never inferred
 * from hard-coded paths.
 *
 * Ordered LAST so the security chain has ALWAYS run before a 304 short-circuit
 * — an unauthenticated request never reaches this filter's 304 path.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class CacheHeadersHandler(
	@param:Value($$"${registry.performance.metadata-max-age-seconds:300}")
	private val maxAgeSeconds: Long,
	private val annotatedEndpoints: AnnotatedEndpointsHandler,
) : WebFilter {
	private val deploymentId = UUID.randomUUID().toString().substring(0, 8)

	private val cacheableEndpoints by lazy { annotatedEndpoints.endpoints(HttpCacheable::class.java) }

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		if (cacheableEndpoints.none { it.matches(request) }) {
			return chain.filter(exchange)
		}

		val locale = request.headers.getFirst(ACCEPT_LANGUAGE) ?: "default"
		val etag = etag(request.path.value(), locale)

		if (request.headers.ifNoneMatch.contains(etag)) {
			writeCacheHeaders(exchange, etag)
			exchange.response.statusCode = NOT_MODIFIED
			return exchange.response.setComplete()
		}

		exchange.response.beforeCommit {
			val status = exchange.response.statusCode
			if (status == null || status.is2xxSuccessful) {
				writeCacheHeaders(exchange, etag)
			}
			Mono.empty()
		}
		return chain.filter(exchange)
	}

	/**
	 * Digested rather than built from [String.hashCode]: a 32-bit collision
	 * between two cacheable paths (or two locales) would revalidate one variant
	 * against the other's body and pin it for the whole max-age.
	 */
	private fun etag(path: String, locale: String): String {
		val digest = MessageDigest.getInstance("SHA-256")
			.digest("$deploymentId|$path|$locale".toByteArray())
			.take(16)
			.joinToString("") { "%02x".format(it) }
		return "\"$digest\""
	}

	private fun writeCacheHeaders(exchange: ServerWebExchange, etag: String) {
		val headers = exchange.response.headers
		headers.eTag = etag
		headers.cacheControl = "private, max-age=$maxAgeSeconds"
		if (headers.vary.none { it.equals(ACCEPT_LANGUAGE, ignoreCase = true) }) {
			headers.vary = headers.vary + ACCEPT_LANGUAGE
		}
	}
}
