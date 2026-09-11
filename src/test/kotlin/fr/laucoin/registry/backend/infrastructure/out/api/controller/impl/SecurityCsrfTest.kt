package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.REFRESH_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

class SecurityCsrfTest : TestContext() {
	@MockitoBean
	private lateinit var authenticationPort: IAuthenticationPort

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val BASE_URL = "/api/v1/authentication"
		private const val CSRF_COOKIE = "XSRF-TOKEN"
		private const val CSRF_HEADER = "X-XSRF-TOKEN"

		private val TOKEN = TokenModel(
			accessToken = "accessToken",
			expiresIn = 3600,
			refreshExpiresIn = 18000,
			refreshToken = "refreshToken",
			tokenType = "Bearer",
		)
	}

	@Test
	fun `Should reject a renewal carrying no CSRF token`() {
		webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()
			.expectStatus().isEqualTo(FORBIDDEN.value())
	}

	@Test
	fun `Should allow opening a session without a CSRF token`() {
		whenever(authenticationPort.getAuthenticationToken(any(), any(), any())).thenReturn(Mono.just(TOKEN))

		val status = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token", emptyList(), emptyList()))
			.bodyValue(mapOf("redirectUri" to "redirectUri", "authorizationCode" to "code", "state" to "aState"))
			.exchange()
			.returnResult(String::class.java)
			.status

		assertNotEquals(FORBIDDEN, status)
	}

	@Test
	fun `Should exempt a request authenticating through the Authorization header`() {
		val status = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.header(AUTHORIZATION, "Bearer aToken")
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()
			.returnResult(String::class.java)
			.status

		assertNotEquals(FORBIDDEN, status)
	}

	@Test
	fun `Should accept the token exactly as the cookie carries it`() {
		// Arrange
		val issued = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), listOf(Pair("redirectUri", "https://app.test"))))
			.exchange()
			.returnResult(String::class.java)
			.responseCookies
			.getFirst(CSRF_COOKIE)

		assertNotNull(issued, "no $CSRF_COOKIE cookie was issued, so the frontend would have nothing to echo")

		// Act
		val status = webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.cookie(CSRF_COOKIE, issued!!.value)
			.header(CSRF_HEADER, issued.value)
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()
			.returnResult(String::class.java)
			.status

		// Assert
		assertNotEquals(FORBIDDEN, status)
	}

	@Test
	fun `Should leave reads alone`() {
		val status = webClient
			.get()
			.uri(uriBuilder("$BASE_URL/login/uri", emptyList(), listOf(Pair("redirectUri", "https://app.test"))))
			.exchange()
			.returnResult(String::class.java)
			.status

		assertNotEquals(FORBIDDEN, status)
	}
}
