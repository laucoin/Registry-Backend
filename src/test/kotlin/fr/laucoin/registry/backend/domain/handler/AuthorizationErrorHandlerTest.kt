package fr.laucoin.registry.backend.domain.handler

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_ALREADY_USED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_EMAIL_NOT_VERIFIED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.INVALID_TOKEN
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ERROR_MESSAGE_PREFIX
import fr.laucoin.registry.backend.domain.model.JwtConversionException
import fr.laucoin.registry.backend.domain.service.ITranslateService
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.web.server.WebFilterExchange
import java.util.stream.Stream

/**
 * The refusals raised while converting the JWT never reach the controller
 * advice — this handler writes them. It used to resolve their message without
 * the exception's arguments, so AUTH_EMAIL_NOT_VERIFIED and
 * AUTH_EMAIL_ALREADY_USED shipped their `{0}` placeholder to the client instead
 * of the address that caused the refusal.
 */
class AuthorizationErrorHandlerTest {
	private val translateService: ITranslateService = mock<ITranslateService>().also {
		whenever(it.getError(any(), anyOrNull(), anyOrNull())).thenReturn("translated")
	}
	private val handler = AuthorizationErrorHandler(translateService, Gson())

	private companion object {
		private const val EMAIL = "nour@example.org"

		@JvmStatic
		fun `Should carry the exception arguments into the resolved message`(): Stream<Arguments> = Stream.of(
			Arguments.of(FORBIDDEN, AUTH_EMAIL_NOT_VERIFIED),
			Arguments.of(CONFLICT, AUTH_EMAIL_ALREADY_USED),
		)
	}

	private fun failureExchange(): Pair<WebFilterExchange, MockServerWebExchange> {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v2/authentication/user/current"))
		val filterExchange = mock<WebFilterExchange>()
		whenever(filterExchange.exchange).thenReturn(exchange)
		return filterExchange to exchange
	}

	private fun capturedArgs(code: String): Array<Any>? {
		val captor = argumentCaptor<Array<Any>>()
		verify(translateService).getError(eq("$ERROR_MESSAGE_PREFIX$code"), captor.capture(), anyOrNull())
		return captor.firstValue
	}

	@ParameterizedTest
	@MethodSource
	fun `Should carry the exception arguments into the resolved message`(status: HttpStatus, code: String) {
		// Arrange
		val (filterExchange, exchange) = failureExchange()

		// Act
		handler.onAuthenticationFailure(filterExchange, JwtConversionException(status, code, arrayListOf(EMAIL)))
			.block()

		// Assert
		assertEquals(status, exchange.response.statusCode)
		assertArrayEquals(arrayOf<Any>(EMAIL), capturedArgs(code))
	}

	@Test
	fun `Should resolve a failure that carries no arguments without any`() {
		// Arrange
		val (filterExchange, exchange) = failureExchange()

		// Act
		handler.onAuthenticationFailure(filterExchange, InvalidBearerTokenException("dead")).block()

		// Assert
		assertEquals(UNAUTHORIZED, exchange.response.statusCode)
		assertNull(capturedArgs(INVALID_TOKEN))
	}
}
