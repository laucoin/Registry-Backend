package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import reactor.test.StepVerifier

class CookieBearerTokenHandlerTest {
	private val converter = CookieBearerTokenHandler()

	@Test
	fun `Should convert resolve the token from the Authorization header when present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v2/whatever").header(AUTHORIZATION, "Bearer headerToken")
		)

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.assertNext { assertEquals("headerToken", (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	@Test
	fun `Should convert fall back to the access token cookie when no header is present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v2/whatever").cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "cookieToken"))
		)

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.assertNext { assertEquals("cookieToken", (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	@Test
	fun `Should convert prefer the Authorization header over the cookie when both are present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v2/whatever")
				.header(AUTHORIZATION, "Bearer headerToken")
				.cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "cookieToken"))
		)

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.assertNext { assertEquals("headerToken", (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	@Test
	fun `Should convert return empty when neither header nor cookie is present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v2/whatever"))

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.verifyComplete()
	}

	@ParameterizedTest
	@ValueSource(
		strings = [
			"/api/v2/authentication/login/uri",
			"/api/v2/authentication/logout/uri",
			"/api/v2/authentication/token",
			"/api/v2/authentication/token/refresh",
		]
	)
	fun `Should convert ignore the access token cookie on a public authentication endpoint`(path: String) {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get(path).cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "staleToken"))
		)

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.verifyComplete()
	}

	@Test
	fun `Should convert read the access token cookie on a protected authentication endpoint`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v2/authentication/user/current").cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "cookieToken"))
		)

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.assertNext { assertEquals("cookieToken", (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	@Test
	fun `Should convert keep the Authorization header on a public authentication endpoint`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/api/v2/authentication/token").header(AUTHORIZATION, "Bearer headerToken")
		)

		// Act
		val result = converter.convert(exchange)

		// Assert
		StepVerifier.create(result)
			.assertNext { assertEquals("headerToken", (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}
}
