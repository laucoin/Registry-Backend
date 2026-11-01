package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import java.time.Instant
import java.time.Duration
import fr.laucoin.registry.backend.test.MutableClock
import java.time.Clock
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import java.util.Locale
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpResponse
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

class AuthenticationRateLimitHandlerTest {
	private companion object {
		const val MAX_TRACKED_CLIENTS = 1000L
	}

	private val objectMapper = JsonMapper.builder().build()
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

	private fun filter(capacity: Int = 2, windowSeconds: Long = 60, clock: Clock = Clock.systemUTC()): AuthenticationRateLimitHandler =
		AuthenticationRateLimitHandler(translateService, localeContextResolver, objectMapper, clock, capacity, windowSeconds, MAX_TRACKED_CLIENTS)

	private fun exchange(
		path: String,
		method: HttpMethod,
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
		val exchange = exchange("/api/v2/authentication/login/uri", GET)

		// Act
		repeat(5) { limiter.filter(exchange, chain).block() }

		// Assert
		assertEquals(5, chainCalls.get())
	}

	@Test
	fun `Should filter allow up to capacity requests then return 429`() {
		// Arrange
		val limiter = filter(capacity = 2)
		val exchange = exchange("/api/v2/authentication/token", POST)

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
	fun `Should filter answer the 429 as a problem detail`() {
		// Arrange
		val limiter = filter(capacity = 1)
		val exchange = exchange("/api/v2/authentication/token", POST)
		limiter.filter(exchange, chain).block()

		// Act
		limiter.filter(exchange, chain).block()

		// Assert
		val body = objectMapper.readValue((exchange.response as MockServerHttpResponse).bodyAsString.block(), Map::class.java)
		assertEquals(APPLICATION_PROBLEM_JSON, exchange.response.headers.contentType)
		assertEquals(429, body["status"])
		assertEquals("429", body["code"])
		assertEquals("about:blank", body["type"])
		assertEquals("/api/v2/authentication/token", body["instance"])
	}

	@Test
	fun `Should filter rate-limit token refresh independently per client IP`() {
		// Arrange
		val limiter = filter(capacity = 1)
		val clientA = exchange("/api/v2/authentication/token/refresh", POST, "10.0.0.1")
		val clientB = exchange("/api/v2/authentication/token/refresh", POST, "10.0.0.2")

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
		val token = exchange("/api/v2/authentication/token", POST, "10.0.0.1")
		val refresh = exchange("/api/v2/authentication/token/refresh", POST, "10.0.0.1")

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
		val exchange = exchange("/api/v2/authentication/token", POST)

		// Act
		limiter.filter(exchange, chain).block()
		Thread.sleep(1100)
		limiter.filter(exchange, chain).block()

		// Assert
		assertEquals(2, chainCalls.get())
	}

	@Test
	fun `Should filter accept requests again once the window has elapsed on the injected clock`() {
		// Arrange
		val clock = MutableClock(Instant.parse("2030-01-01T00:00:00Z"))
		val limiter = filter(capacity = 1, windowSeconds = 60, clock = clock)
		val exchange = exchange("/api/v2/authentication/token", POST)
		limiter.filter(exchange, chain).block()
		limiter.filter(exchange, chain).block()

		// Act
		clock.advance(Duration.ofSeconds(61))
		limiter.filter(exchange, chain).block()

		// Assert
		assertEquals(2, chainCalls.get())
	}
}
