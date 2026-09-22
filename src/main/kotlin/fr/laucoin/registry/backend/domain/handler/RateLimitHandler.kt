package fr.laucoin.registry.backend.domain.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.annotation.RateLimited
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_TITLE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.RateLimitCategoryEnum
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.ErrorDto
import java.security.Principal
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono

/**
 * Enforces [RateLimited] on v2 controller-interface methods. Unlike
 * [AuthenticationRateLimitHandler] (which matches on a fixed, pre-auth path
 * allowlist), this resolves the exchange to its [HandlerMethod] through the
 * app's [RequestMappingHandlerMapping] and reads the annotation off it —
 * [AnnotationUtils.findAnnotation] walks up to the declaring interface, which
 * is where `@RateLimited` actually lives (the impl only overrides the
 * method). Counted per authenticated principal, not remote address: these are
 * endpoints reached after authentication, and an office NAT should not share
 * one budget across every user behind it.
 */
class RateLimitHandler(
	private val handlerMapping: RequestMappingHandlerMapping,
	private val translateService: ITranslateService,
	private val localeContextResolver: LocaleContextResolver,
	private val gson: Gson,
	private val capacities: Map<RateLimitCategoryEnum, Int>,
	private val windowSeconds: Map<RateLimitCategoryEnum, Long>,
) : WebFilter {
	private val counters: Cache<String, SlidingWindowCounter> = Caffeine.newBuilder()
		.expireAfterAccess(Duration.ofSeconds((windowSeconds.values.maxOrNull() ?: 60L) * EXPIRY_GRACE_MULTIPLIER))
		.build()

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		// A resolved Mono<Void> never emits onNext even on a match, so switchIfEmpty chained after a
		// Mono<Void>-returning flatMap can't tell "no handler matched" apart from "a handler matched and
		// its (voidreturning) response completed normally" — both look empty and it fires either way,
		// invoking the filter chain twice. Resolving the annotation to a plain (non-reactive) value first
		// and defaultIfEmpty-ing that keeps exactly one Mono<Void>-producing step, run exactly once.
		return handlerMapping.getHandler(exchange)
			.map { handler -> Optional.ofNullable(resolveRateLimited(handler)) }
			.defaultIfEmpty(Optional.empty())
			.flatMap { rateLimited ->
				val annotation = rateLimited.orElse(null)
				if (annotation == null || !isEnforced(annotation, exchange)) {
					chain.filter(exchange)
				} else {
					enforce(annotation, exchange, chain)
				}
			}
	}

	private fun resolveRateLimited(handler: Any): RateLimited? {
		val method = (handler as? HandlerMethod)?.method
		return method?.let { AnnotationUtils.findAnnotation(it, RateLimited::class.java) }
	}

	private fun isEnforced(rateLimited: RateLimited, exchange: ServerWebExchange): Boolean {
		val capacity = capacities[rateLimited.category] ?: return false
		if (capacity <= 0) return false
		if (rateLimited.whenParamPresent.isEmpty()) return true
		return rateLimited.whenParamPresent.any { exchange.request.queryParams.containsKey(it) }
	}

	private fun enforce(rateLimited: RateLimited, exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val category = rateLimited.category
		val capacity = capacities.getValue(category)
		val window = windowSeconds[category] ?: DEFAULT_WINDOW_SECONDS

		return exchange.getPrincipal<Principal>()
			.map { it.name }
			.defaultIfEmpty(exchange.request.remoteAddress?.address?.hostAddress ?: "unknown")
			.flatMap { principal ->
				val allowed = counters.get("$principal|$category") {
					SlidingWindowCounter(capacity, Duration.ofSeconds(window))
				}.tryConsume()

				if (allowed) chain.filter(exchange) else tooManyRequests(exchange, window)
			}
	}

	private fun tooManyRequests(exchange: ServerWebExchange, windowSeconds: Long): Mono<Void> {
		val response = exchange.response
		response.statusCode = TOO_MANY_REQUESTS
		response.headers.contentType = APPLICATION_JSON
		response.headers.add(RETRY_AFTER, windowSeconds.toString())

		val locale = localeContextResolver.resolveLocaleContext(exchange).locale ?: Locale.getDefault()
		val error = ErrorDto(
			statusCode = TOO_MANY_REQUESTS.value(),
			statusName = TOO_MANY_REQUESTS.name,
			code = TOO_MANY_REQUESTS.value().toString(),
			title = translateService.getError(code = "$ERROR_TITLE_PREFIX${TOO_MANY_REQUESTS.value()}", locale = locale),
			message = translateService.getError(
				code = "$ERROR_MESSAGE_PREFIX${TOO_MANY_REQUESTS.value()}",
				locale = locale,
			),
		)

		return response.writeWith(Mono.just(response.bufferFactory().wrap(gson.toJson(error).toByteArray())))
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
		const val EXPIRY_GRACE_MULTIPLIER = 10L
		const val DEFAULT_WINDOW_SECONDS = 60L
	}
}
