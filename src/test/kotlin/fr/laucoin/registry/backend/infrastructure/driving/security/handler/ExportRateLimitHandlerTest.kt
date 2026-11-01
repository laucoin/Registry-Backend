package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.domain.enumeration.AuditActionEnum.EXPORT
import fr.laucoin.registry.backend.domain.enumeration.AuditOutcomeEnum.RATE_LIMITED
import fr.laucoin.registry.backend.domain.enumeration.AuditResourceTypeEnum
import fr.laucoin.registry.backend.domain.model.AuditModel
import fr.laucoin.registry.backend.domain.model.ClientContextModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.service.IAuditService
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TooManyRequestsResponder
import fr.laucoin.registry.backend.test.MutableClock
import java.net.InetSocketAddress
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.http.HttpHeaders.RETRY_AFTER
import org.springframework.http.HttpMethod
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
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

class ExportRateLimitHandlerTest {
	private val auditService: IAuditService = mock()
	private val translateService: ITranslateService = mock()
	private val localeContextResolver: LocaleContextResolver = mock()
	private val clock = MutableClock(Instant.parse("2026-01-01T10:00:00Z"))
	private val chainCalls = AtomicInteger(0)
	private val chain = WebFilterChain {
		chainCalls.incrementAndGet()
		it.response.statusCode = OK
		Mono.empty()
	}
	private val projectId = UUID.randomUUID()
	private val participantId = UUID.randomUUID()

	init {
		whenever(translateService.getError(any(), anyOrNull(), anyOrNull(), any())).thenReturn("")
		whenever(localeContextResolver.resolveLocaleContext(any())).thenReturn(SimpleLocaleContext(Locale.ENGLISH))
	}

	private fun limiter(capacity: Int = 2, windowSeconds: Long = 60) = ExportRateLimitHandler(
		auditService,
		ClientContextService(emptyList()),
		TooManyRequestsResponder(translateService, localeContextResolver, JsonMapper.builder().build()),
		clock,
		capacity,
		windowSeconds,
		1000L,
	)

	private fun exchange(path: String, method: HttpMethod = GET): ServerWebExchange {
		val request = MockServerHttpRequest.method(method, path)
			.remoteAddress(InetSocketAddress("203.0.113.7", 12345))
			.header("User-Agent", "JUnit/1.0")
			.build()
		return MockServerWebExchange.from(request)
	}

	private fun call(limiter: ExportRateLimitHandler, exchange: ServerWebExchange, user: UUID?) {
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
		val exchange = exchange("/api/v2/users/current/export")

		// Act
		repeat(3) { call(limiter, exchange, user) }

		// Assert
		assertEquals(2, chainCalls.get())
		assertEquals(TOO_MANY_REQUESTS, exchange.response.statusCode)
		assertEquals("90", exchange.response.headers.getFirst(RETRY_AFTER))
	}

	@Test
	fun `Should filter share one budget across every export endpoint of a user`() {
		// Arrange
		val limiter = limiter(capacity = 2)
		val user = UUID.randomUUID()
		val participantPath = "/api/v2/projects/$projectId/participants/$participantId/export"

		// Act
		call(limiter, exchange("/api/v2/users/current/export"), user)
		call(limiter, exchange("/api/v2/users/${UUID.randomUUID()}/export"), user)
		val third = exchange(participantPath)
		call(limiter, third, user)

		// Assert
		assertEquals(TOO_MANY_REQUESTS, third.response.statusCode)
		assertEquals(2, chainCalls.get())
	}

	@Test
	fun `Should filter isolate the budget of each user`() {
		// Arrange
		val limiter = limiter(capacity = 1)

		// Act
		call(limiter, exchange("/api/v2/users/current/export"), UUID.randomUUID())
		call(limiter, exchange("/api/v2/users/current/export"), UUID.randomUUID())

		// Assert
		assertEquals(2, chainCalls.get())
		verify(auditService, never()).record(any())
	}

	@Test
	fun `Should filter count HEAD requests in the same budget as GET`() {
		// Arrange
		val limiter = limiter(capacity = 1)
		val user = UUID.randomUUID()
		call(limiter, exchange("/api/v2/users/current/export"), user)
		val head = exchange("/api/v2/users/current/export", HEAD)

		// Act
		call(limiter, head, user)

		// Assert
		assertEquals(TOO_MANY_REQUESTS, head.response.statusCode)
		assertEquals(1, chainCalls.get())
	}

	@Test
	fun `Should filter ignore other paths and other methods`() {
		// Arrange
		val limiter = limiter(capacity = 1)
		val user = UUID.randomUUID()

		// Act
		repeat(3) {
			call(limiter, exchange("/api/v2/users/current"), user)
			call(limiter, exchange("/api/v2/users/current/export", POST), user)
		}

		// Assert
		assertEquals(6, chainCalls.get())
	}

	@Test
	fun `Should filter let an unauthenticated request through`() {
		// Arrange
		val limiter = limiter(capacity = 1)

		// Act
		repeat(3) { call(limiter, exchange("/api/v2/users/current/export"), user = null) }

		// Assert
		assertEquals(3, chainCalls.get())
	}

	@Test
	fun `Should filter allow again once the window has elapsed`() {
		// Arrange
		val limiter = limiter(capacity = 1, windowSeconds = 60)
		val user = UUID.randomUUID()
		call(limiter, exchange("/api/v2/users/current/export"), user)
		call(limiter, exchange("/api/v2/users/current/export"), user)

		// Act
		clock.advance(Duration.ofSeconds(61))
		val exchange = exchange("/api/v2/users/current/export")
		call(limiter, exchange, user)

		// Assert
		assertEquals(OK, exchange.response.statusCode)
		assertEquals(2, chainCalls.get())
	}

	@Test
	fun `Should filter audit a refused participant export with the client context`() {
		// Arrange
		val limiter = limiter(capacity = 1)
		val user = UUID.randomUUID()
		val path = "/api/v2/projects/$projectId/participants/$participantId/export"
		call(limiter, exchange(path), user)

		// Act
		call(limiter, exchange(path), user)

		// Assert
		verify(auditService).record(
			AuditModel(
				EXPORT, AuditResourceTypeEnum.PARTICIPANT, participantId, projectId, user, RATE_LIMITED,
				ClientContextModel("203.0.113.7", "JUnit/1.0"), ZonedDateTime.parse("2026-01-01T10:00:00Z"),
			)
		)
	}

	@Test
	fun `Should filter audit a refused current user export on the user itself`() {
		// Arrange
		val limiter = limiter(capacity = 1)
		val user = UUID.randomUUID()
		call(limiter, exchange("/api/v2/users/current/export"), user)

		// Act
		call(limiter, exchange("/api/v2/users/current/export"), user)

		// Assert
		verify(auditService).record(
			AuditModel(
				EXPORT, AuditResourceTypeEnum.USER, user, null, user, RATE_LIMITED,
				ClientContextModel("203.0.113.7", "JUnit/1.0"), ZonedDateTime.parse("2026-01-01T10:00:00Z"),
			)
		)
	}
}
