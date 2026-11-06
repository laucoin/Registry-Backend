package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TooManyRequestsResponder
import fr.laucoin.registry.backend.test.MutableClock
import java.time.Duration
import java.time.Instant
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import org.springframework.web.server.i18n.LocaleContextResolver
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper

class UserSearchRateLimitHandlerTest {
	private val translateService: ITranslateService = mock()
	private val localeContextResolver: LocaleContextResolver = mock()
	private val clock = MutableClock(Instant.parse("2026-01-01T10:00:00Z"))
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

	private fun limiter(capacity: Int = 2, windowSeconds: Long = 60) = UserSearchRateLimitHandler(
		TooManyRequestsResponder(translateService, localeContextResolver, JsonMapper.builder().build()),
		clock,
		capacity,
		windowSeconds,
		1000L,
	)

	private fun searchPath(projectId: UUID = UUID.randomUUID()) = "/api/v2/projects/$projectId/profiles/search/users"

	private fun exchange(path: String, method: HttpMethod = GET): ServerWebExchange =
		MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build())

	private fun call(limiter: UserSearchRateLimitHandler, exchange: ServerWebExchange, user: UUID?) {
		val flow = limiter.filter(exchange, chain)
		val authenticated = user?.let {
			val principal = CurrentUserModel().apply { id = it }
			flow.contextWrite(ReactiveSecurityContextHolder.withAuthentication(UsernamePasswordAuthenticationToken(principal, null)))
		} ?: flow
		authenticated.block()
	}

	@Test
	fun `Should filter answer 429 with Retry-After once the capacity is exceeded`() {
		// Arrange
		val limiter = limiter(capacity = 2, windowSeconds = 90)
		val user = UUID.randomUUID()
		val exchange = exchange(searchPath())

		// Act
		repeat(3) { call(limiter, exchange, user) }

		// Assert
		assertEquals(2, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, exchange.response.statusCode)
		assertEquals("90", exchange.response.headers.getFirst(RETRY_AFTER))
	}

	@Test
	fun `Should filter share one budget across the projects of a user`() {
		// Arrange
		val limiter = limiter(capacity = 1)
		val user = UUID.randomUUID()
		val other = exchange(searchPath())

		// Act
		call(limiter, exchange(searchPath()), user)
		call(limiter, other, user)

		// Assert
		assertEquals(1, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, other.response.statusCode)
	}

	@Test
	fun `Should filter isolate the budget of each user`() {
		// Arrange
		val limiter = limiter(capacity = 1)

		// Act
		call(limiter, exchange(searchPath()), UUID.randomUUID())
		call(limiter, exchange(searchPath()), UUID.randomUUID())

		// Assert
		assertEquals(2, chainCalls.get())
	}

	@Test
	fun `Should filter accept searches again once the window has elapsed`() {
		// Arrange
		val limiter = limiter(capacity = 1, windowSeconds = 60)
		val user = UUID.randomUUID()
		call(limiter, exchange(searchPath()), user)
		call(limiter, exchange(searchPath()), user)

		// Act
		clock.advance(Duration.ofSeconds(61))
		call(limiter, exchange(searchPath()), user)

		// Assert
		assertEquals(2, chainCalls.get())
	}

	@Test
	fun `Should filter ignore other paths, other methods and unauthenticated calls`() {
		// Arrange
		val limiter = limiter(capacity = 1)
		val user = UUID.randomUUID()

		// Act
		repeat(2) { call(limiter, exchange("/api/v2/projects/${UUID.randomUUID()}/profiles"), user) }
		repeat(2) { call(limiter, exchange(searchPath(), POST), user) }
		repeat(2) { call(limiter, exchange(searchPath()), user = null) }

		// Assert
		assertEquals(6, chainCalls.get())
	}
}
