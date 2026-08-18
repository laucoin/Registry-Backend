package fr.laucoin.registry.backend.domain.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.ErrorConst.RATE_LIMIT_EXCEEDED
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.ErrorDto
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.security.Principal
import java.time.Duration
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * Backend-tier rate limiting: fine-grained, per-authenticated-user
 * limits on the sensitive and expensive operations, enforced with non-blocking
 * in-process buckets (Bucket4j token buckets keyed through a Caffeine cache, so
 * the WebFlux event loop never blocks and no shared store is needed). Limits
 * are per-replica by design — the edge tier owns the global volumetric limit.
 *
 * Two categories, both deploy-tunable (capacity 0 disables a category):
 *  - SENSITIVE — mutating state transitions (block/disable/…),
 *    deletes and purge jobs
 *  - SEARCH — the trigram-backed pickers and q= searches
 *
 * An endpoint is limited exclusively when its contract method declares
 * [RateLimited] — the behaviour is visible on the endpoint, never inferred
 * from hard-coded paths.
 *
 * Over-limit responds 429 + Retry-After with the localized ErrorDto shape.
 * Ordered after the security chain so the JWT principal is available; keys
 * fall back to the client IP on the few unauthenticated paths.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class RateLimitHandler(
	private val translateService: ITranslateService,
	private val gson: Gson,
	private val annotatedEndpoints: AnnotatedEndpointsHandler,
	@param:Value($$"${registry.security.rate-limit.sensitive.capacity:20}")
	private val sensitiveCapacity: Long,
	@param:Value($$"${registry.security.rate-limit.sensitive.period-seconds:60}")
	private val sensitivePeriodSeconds: Long,
	@param:Value($$"${registry.security.rate-limit.search.capacity:60}")
	private val searchCapacity: Long,
	@param:Value($$"${registry.security.rate-limit.search.period-seconds:60}")
	private val searchPeriodSeconds: Long,
) : WebFilter {
	private data class Category(val name: String, val capacity: Long, val periodSeconds: Long)

	private val limitedEndpoints by lazy { annotatedEndpoints.endpoints(RateLimited::class.java) }

	private val buckets: Cache<String, Bucket> = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofMinutes(10))
		.maximumSize(100_000)
		.build()

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val category = categorize(exchange.request) ?: return chain.filter(exchange)
		if (category.capacity <= 0) {
			return chain.filter(exchange)
		}

		return exchange.getPrincipal<Principal>()
			.map { it.name }
			.defaultIfEmpty(exchange.request.remoteAddress?.address?.hostAddress ?: "unknown")
			.flatMap { key ->
				val bucket = buckets.get("${category.name}|$key") { newBucket(category) }
				val probe = bucket.tryConsumeAndReturnRemaining(1)
				if (probe.isConsumed) {
					chain.filter(exchange)
				} else {
					tooManyRequests(exchange, probe.nanosToWaitForRefill)
				}
			}
	}

	private fun categorize(request: ServerHttpRequest): Category? {
		val limited = limitedEndpoints
			.firstOrNull { it.matches(request) && it.annotation.appliesTo(request) }
			?: return null
		return when (limited.annotation.category) {
			RateLimitCategoryEnum.SENSITIVE -> Category("sensitive", sensitiveCapacity, sensitivePeriodSeconds)
			RateLimitCategoryEnum.SEARCH -> Category("search", searchCapacity, searchPeriodSeconds)
		}
	}

	private fun RateLimited.appliesTo(request: ServerHttpRequest): Boolean =
		whenParamPresent.isEmpty() || whenParamPresent.any { request.queryParams.containsKey(it) }

	private fun newBucket(category: Category): Bucket = Bucket.builder()
		.addLimit(
			Bandwidth.builder()
				.capacity(category.capacity)
				.refillGreedy(category.capacity, Duration.ofSeconds(category.periodSeconds))
				.build()
		)
		.build()

	private fun tooManyRequests(exchange: ServerWebExchange, nanosToWaitForRefill: Long): Mono<Void> {
		val retryAfterSeconds = NANOSECONDS.toSeconds(nanosToWaitForRefill).coerceAtLeast(1)
		val response = exchange.response
		response.statusCode = TOO_MANY_REQUESTS
		response.headers.contentType = APPLICATION_JSON
		response.headers.set("Retry-After", retryAfterSeconds.toString())

		val error = ErrorDto(
			statusCode = TOO_MANY_REQUESTS.value(),
			statusName = TOO_MANY_REQUESTS.name,
			code = RATE_LIMIT_EXCEEDED,
			title = translateService.getError(code = "$ERROR_TITLE_PREFIX${TOO_MANY_REQUESTS.value()}"),
			message = translateService.getError(
				code = "$ERROR_MESSAGE_PREFIX$RATE_LIMIT_EXCEEDED",
				args = arrayOf(retryAfterSeconds),
			),
		)
		return response.writeWith(Mono.just(response.bufferFactory().wrap(gson.toJson(error).toByteArray())))
	}
}
