package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.REFRESH_TOKEN_COOKIE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class AuthenticationCookieServiceTest {
	private val service = AuthenticationCookieService(apiPrefix = "/api", isSecure = true)

	@Test
	fun `Should setAuthCookies scope the refresh cookie to the v2 authentication path`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
		val token = TokenModel(accessToken = "access", expiresIn = 60, refreshToken = "refresh", tokenType = "Bearer")

		// Act
		service.setAuthCookies(exchange.response, token)

		// Assert
		assertEquals("/api", exchange.response.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.path)
		assertEquals("/api/v2/authentication", exchange.response.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.path)
	}

	@Test
	fun `Should clearAuthCookies scope the refresh cookie to the v2 authentication path`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/"))

		// Act
		service.clearAuthCookies(exchange.response)

		// Assert
		assertEquals("/api/v2/authentication", exchange.response.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.path)
	}
}
