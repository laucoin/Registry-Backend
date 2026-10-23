package fr.laucoin.registry.backend.infrastructure.driving.security.service

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.REFRESH_TOKEN_COOKIE
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class AuthenticationCookieServiceTest {
	private val service = AuthenticationCookieService("/api", secure = false)

	private fun response(): ServerHttpResponse =
		MockServerWebExchange.from(MockServerHttpRequest.get("/")).response

	@Test
	fun `Should scope the refresh cookie to the authentication sub-tree of the current api version`() {
		// Arrange
		val response = response()

		// Act
		service.setAuthCookies(response, TokenModel("access", 60, "refresh", "Bearer"))

		// Assert
		assertEquals("/api", response.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.path)
		assertEquals("/api/v2/authentication", response.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.path)
	}

	@Test
	fun `Should clear the refresh cookie on the same path it was set`() {
		// Arrange
		val response = response()

		// Act
		service.clearAuthCookies(response)

		// Assert
		assertEquals("/api/v2/authentication", response.cookies.getFirst(REFRESH_TOKEN_COOKIE)?.path)
	}
}
