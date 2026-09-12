package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.GET
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class CsrfTokenServiceTest {
	private val service = CsrfTokenService(hmacSecret = "test-secret")

	@Test
	fun `Should computeToken return the same value for the same access token`() {
		// Arrange
		val exchangeA = exchangeWithAccessTokenCookie("accessToken")
		val exchangeB = exchangeWithAccessTokenCookie("accessToken")

		// Act & Assert
		assertEquals(service.computeToken(exchangeA), service.computeToken(exchangeB))
	}

	@Test
	fun `Should computeToken return a different value for a different access token`() {
		// Arrange
		val exchangeA = exchangeWithAccessTokenCookie("accessTokenA")
		val exchangeB = exchangeWithAccessTokenCookie("accessTokenB")

		// Act & Assert
		assertNotEquals(service.computeToken(exchangeA), service.computeToken(exchangeB))
	}

	@Test
	fun `Should computeToken derive from the Authorization bearer header when present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.method(GET, "/api/v1/whatever").header(AUTHORIZATION, "Bearer accessToken")
		)
		val cookieExchange = exchangeWithAccessTokenCookie("accessToken")

		// Act & Assert
		assertEquals(service.computeToken(cookieExchange), service.computeToken(exchange))
	}

	@Test
	fun `Should computeToken prefer the Authorization header over the cookie when both are present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.method(GET, "/api/v1/whatever")
				.header(AUTHORIZATION, "Bearer headerToken")
				.cookie(HttpCookie(ACCESS_TOKEN_COOKIE, "cookieToken"))
		)
		val headerOnlyExchange = MockServerWebExchange.from(
			MockServerHttpRequest.method(GET, "/api/v1/whatever").header(AUTHORIZATION, "Bearer headerToken")
		)

		// Act & Assert
		assertEquals(service.computeToken(headerOnlyExchange), service.computeToken(exchange))
	}

	@Test
	fun `Should computeToken still return a value when no token is present`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.method(GET, "/api/v1/whatever"))

		// Act & Assert
		assertEquals(service.computeToken(exchange), service.computeToken(exchange))
	}

	@Test
	fun `Should generateToken and loadToken return the same deterministic token`() {
		// Arrange
		val exchange = exchangeWithAccessTokenCookie("accessToken")

		// Act
		val generated = service.generateToken(exchange).block()
		val loaded = service.loadToken(exchange).block()

		// Assert
		assertEquals(generated!!.token, loaded!!.token)
		assertEquals(CsrfTokenService.HEADER_NAME, generated.headerName)
		assertEquals(CsrfTokenService.PARAMETER_NAME, generated.parameterName)
	}

	@Test
	fun `Should saveToken be a no-op`() {
		// Arrange
		val exchange = exchangeWithAccessTokenCookie("accessToken")
		val token = service.generateToken(exchange).block()

		// Act
		val result = service.saveToken(exchange, token).block()

		// Assert
		assertNull(result)
	}

	private fun exchangeWithAccessTokenCookie(value: String) = MockServerWebExchange.from(
		MockServerHttpRequest.method(GET, "/api/v1/whatever").cookie(HttpCookie(ACCESS_TOKEN_COOKIE, value))
	)
}
