package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken

class CookieBearerTokenHandlerTest {
	private val converter = CookieBearerTokenHandler()

	@Test
	fun `Should convert resolve the token from the Authorization header when present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v1/whatever").header(AUTHORIZATION, "Bearer headerToken")
		)

		// Act
		val result = converter.convert(exchange).block() as BearerTokenAuthenticationToken

		// Assert
		assertEquals("headerToken", result.token)
	}

	@Test
	fun `Should convert fall back to the access token cookie when no header is present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v1/whatever").cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "cookieToken"))
		)

		// Act
		val result = converter.convert(exchange).block() as BearerTokenAuthenticationToken

		// Assert
		assertEquals("cookieToken", result.token)
	}

	@Test
	fun `Should convert prefer the Authorization header over the cookie when both are present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v1/whatever")
				.header(AUTHORIZATION, "Bearer headerToken")
				.cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "cookieToken"))
		)

		// Act
		val result = converter.convert(exchange).block() as BearerTokenAuthenticationToken

		// Assert
		assertEquals("headerToken", result.token)
	}

	@Test
	fun `Should convert return empty when neither header nor cookie is present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/whatever"))

		// Act
		val result = converter.convert(exchange).block()

		// Assert
		assertNull(result)
	}
}
