package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import java.util.Locale
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono

class AuthenticationRateLimitHandlerTest {
	private val translateService: ITranslateService = mock()
	private val localeContextResolver: LocaleContextResolver = mock()
	private val chainCalls = AtomicInteger(0)
	private val chain = WebFilterChain {
		chainCalls.incrementAndGet()
		it.response.statusCode = OK
		Mono.empty()
	}

	init {
		whenever(translateService.getError(any(), anyOrNull(), anyOrNull(), any())).thenReturn("")
		whenever(localeContextResolver.resolveLocaleContext(any())).thenReturn(SimpleLocaleContext(Locale.ENGLISH))
	}

	private fun filter(capacity: Int = 2, windowSeconds: Long = 60): AuthenticationRateLimitHandler =
		AuthenticationRateLimitHandler(translateService, localeContextResolver, Gson(), capacity, windowSeconds)

	private fun exchange(
		path: String,
		method: org.springframework.http.HttpMethod,
		remoteHost: String = "10.0.0.1",
	): ServerWebExchange {
		val request = MockServerHttpRequest.method(method, path)
			.remoteAddress(InetSocketAddress(remoteHost, 12345))
			.build()
		return MockServerWebExchange.from(request)
	}

	@Test
	fun `Should filter let the request through when the path is not rate-limited`() {
		// Arrange
		val limiter = filter(capacity = 1)
		val exchange = exchange("/api/v1/authentication/login/uri", GET)

		// Act
		repeat(5) { limiter.filter(exchange, chain).block() }

		// Assert
		assertEquals(5, chainCalls.get())
	}

	@Test
	fun `Should filter allow up to capacity requests then return 429`() {
		// Arrange
		val limiter = filter(capacity = 2)
		val exchange = exchange("/api/v1/authentication/token", POST)

		// Act
		limiter.filter(exchange, chain).block()
		limiter.filter(exchange, chain).block()
		limiter.filter(exchange, chain).block()

		// Assert
		assertEquals(2, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, exchange.response.statusCode)
		assertEquals("60", exchange.response.headers.getFirst(RETRY_AFTER))
	}

	@Test
	fun `Should filter rate-limit token refresh independently per client IP`() {
		// Arrange
		val limiter = filter(capacity = 1)
		val clientA = exchange("/api/v1/authentication/token/refresh", POST, "10.0.0.1")
		val clientB = exchange("/api/v1/authentication/token/refresh", POST, "10.0.0.2")

		// Act
		limiter.filter(clientA, chain).block()
		limiter.filter(clientB, chain).block()
		limiter.filter(clientA, chain).block()

		// Assert
		assertEquals(2, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, clientA.response.statusCode)
		assertEquals(OK, clientB.response.statusCode)
	}

	@Test
	fun `Should filter track token and token refresh as independent budgets for the same client`() {
		// Arrange
		val limiter = filter(capacity = 1)
		val token = exchange("/api/v1/authentication/token", POST, "10.0.0.1")
		val refresh = exchange("/api/v1/authentication/token/refresh", POST, "10.0.0.1")

		// Act
		limiter.filter(token, chain).block()
		limiter.filter(refresh, chain).block()

		// Assert
		assertEquals(2, chainCalls.get())
		assertEquals(OK, token.response.statusCode)
		assertEquals(OK, refresh.response.statusCode)
	}

	@Test
	fun `Should filter allow requests again once the window has elapsed`() {
		// Arrange
		val limiter = filter(capacity = 1, windowSeconds = 1)
		val exchange = exchange("/api/v1/authentication/token", POST)

		// Act
		limiter.filter(exchange, chain).block()
		Thread.sleep(1100)
		limiter.filter(exchange, chain).block()

		// Assert
		assertEquals(2, chainCalls.get())
	}
}
