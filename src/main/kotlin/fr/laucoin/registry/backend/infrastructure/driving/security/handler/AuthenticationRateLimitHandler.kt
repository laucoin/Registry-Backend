package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.ErrorDto
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import tools.jackson.databind.ObjectMapper

/**
 * Purpose: Rate limits the unauthenticated token endpoints.
 * Scope: Counts POST calls per client address and path in a sliding window and answers 429 beyond the capacity.
 * Limits: In memory and per instance; the tracked clients are bounded.
 */
class AuthenticationRateLimitHandler(
	private val translateService: ITranslateService,
	private val localeContextResolver: LocaleContextResolver,
	private val objectMapper: ObjectMapper,
	private val capacity: Int,
	private val windowSeconds: Long,
	maxTrackedClients: Long,
) : WebFilter {
	private val pathMatcher = AntPathMatcher()

	private val counters: Cache<String, SlidingWindowCounter> = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofSeconds(windowSeconds * EXPIRY_GRACE_MULTIPLIER))
		.maximumSize(maxTrackedClients)
		.build()

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val request = exchange.request
		if (request.method != POST) return chain.filter(exchange)

		val matchedPath = RATE_LIMITED_PATHS.firstOrNull { pathMatcher.match(it, request.path.value()) }
			?: return chain.filter(exchange)

		val clientId = request.remoteAddress?.address?.hostAddress ?: return chain.filter(exchange)
		val allowed = counters.get("$clientId|$matchedPath") {
			SlidingWindowCounter(capacity, Duration.ofSeconds(windowSeconds))
		}.tryConsume()

		return if (allowed) chain.filter(exchange) else tooManyRequests(exchange)
	}

	private fun tooManyRequests(exchange: ServerWebExchange): Mono<Void> {
		val response = exchange.response
		response.statusCode = TOO_MANY_REQUESTS
		response.headers.contentType = APPLICATION_PROBLEM_JSON
		response.headers.add(RETRY_AFTER, windowSeconds.toString())

		val locale = localeContextResolver.resolveLocaleContext(exchange).locale ?: Locale.getDefault()
		val error = ErrorDto(
			title = translateService.getError(
				code = "$ERROR_TITLE_PREFIX${TOO_MANY_REQUESTS.value()}",
				locale = locale
			),
			status = TOO_MANY_REQUESTS.value(),
			detail = translateService.getError(
				code = "$ERROR_MESSAGE_PREFIX${TOO_MANY_REQUESTS.value()}",
				locale = locale
			),
			instance = exchange.request.path.value(),
			code = TOO_MANY_REQUESTS.value().toString(),
		)

		return response.writeWith(Mono.just(response.bufferFactory().wrap(objectMapper.writeValueAsBytes(error))))
	}

	private class SlidingWindowCounter(private val capacity: Int, private val window: Duration) {
		private val state = AtomicReference(WindowState(Instant.now(), 0))

		fun tryConsume(): Boolean {
			while (true) {
				val current = state.get()
				val now = Instant.now()
				val next = if (Duration.between(current.start, now) >= window) {
					WindowState(now, 1)
				} else {
					WindowState(current.start, current.count + 1)
				}
				if (state.compareAndSet(current, next)) {
					return next.count <= capacity
				}
			}
		}

		private data class WindowState(val start: Instant, val count: Int)
	}

	private companion object {
		val RATE_LIMITED_PATHS = listOf("/api/*/authentication/token", "/api/*/authentication/token/refresh")
		const val EXPIRY_GRACE_MULTIPLIER = 10L
	}
}
