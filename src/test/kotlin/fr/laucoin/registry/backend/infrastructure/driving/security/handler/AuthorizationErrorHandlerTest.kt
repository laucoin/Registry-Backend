package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.constant.ErrorConst.FAILED_TO_LOGIN_FOR_UNKNOWN_REASON
import fr.laucoin.registry.backend.domain.constant.ErrorConst.INVALID_TOKEN
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_AUTHENTICATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.context.i18n.SimpleLocaleContext
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.LOCKED
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.i18n.LocaleContextResolver

class AuthorizationErrorHandlerTest {
	private val translateService: ITranslateService = mock()
	private val localeContextResolver: LocaleContextResolver = mock()
	private val handler = AuthorizationErrorHandler(translateService, localeContextResolver, Gson())

	init {
		whenever(translateService.getError(any(), anyOrNull(), anyOrNull(), any())).thenReturn("translated")
		whenever(localeContextResolver.resolveLocaleContext(any())).thenReturn(SimpleLocaleContext(Locale.ENGLISH))
	}

	private fun exchange() = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v2/users/current"))

	private fun assertProblem(exchange: MockServerWebExchange, status: HttpStatus, code: String) {
		val body = Gson().fromJson(exchange.response.bodyAsString.block(), Map::class.java)

		assertEquals(status, exchange.response.statusCode)
		assertEquals(APPLICATION_PROBLEM_JSON, exchange.response.headers.contentType)
		assertEquals("about:blank", body["type"])
		assertEquals(status.value().toDouble(), body["status"])
		assertEquals(code, body["code"])
		assertEquals("/api/v2/users/current", body["instance"])
		assertEquals("translated", body["detail"])
		assertNotNull(body["title"])
	}

	private fun fail(exchange: MockServerWebExchange, exception: AuthenticationException) {
		handler.onAuthenticationFailure(WebFilterExchange(exchange) { reactor.core.publisher.Mono.empty() }, exception).block()
	}

	@Test
	fun `Should answer an invalid bearer token with a 401 problem`() {
		// Arrange
		val exchange = exchange()

		// Act
		fail(exchange, InvalidBearerTokenException("expired"))

		// Assert
		assertProblem(exchange, UNAUTHORIZED, INVALID_TOKEN)
	}

	@Test
	fun `Should answer a conversion failure with its own status and code`() {
		// Arrange
		val exchange = exchange()

		// Act
		fail(exchange, JwtConversionException(LOCKED, "AUTH_BLOCKED_ACCOUNT"))

		// Assert
		assertProblem(exchange, LOCKED, "AUTH_BLOCKED_ACCOUNT")
	}

	@Test
	fun `Should answer any other authentication failure with a 500 problem`() {
		// Arrange
		val exchange = exchange()

		// Act
		fail(exchange, BadCredentialsException("bad"))

		// Assert
		assertProblem(exchange, INTERNAL_SERVER_ERROR, FAILED_TO_LOGIN_FOR_UNKNOWN_REASON)
	}

	@Test
	fun `Should answer an anonymous request with a 401 problem`() {
		// Arrange
		val exchange = exchange()

		// Act
		handler.unauthorizedHandler().commence(exchange, BadCredentialsException("anonymous")).block()

		// Assert
		assertProblem(exchange, UNAUTHORIZED, NOT_AUTHENTICATED)
	}

	@Test
	fun `Should answer a denied access with a 403 problem`() {
		// Arrange
		val exchange = exchange()

		// Act
		handler.accessDeniedHandler().handle(exchange, org.springframework.security.access.AccessDeniedException("denied")).block()

		// Assert
		assertProblem(exchange, FORBIDDEN, NOT_ENOUGH_PERMISSION)
	}
}
