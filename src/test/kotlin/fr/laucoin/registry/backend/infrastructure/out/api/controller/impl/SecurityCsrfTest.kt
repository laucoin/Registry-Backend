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

/**
 * Whether CSRF is enforced, and where it deliberately is not.
 *
 * The other contract tests attach a token through `WebTestClientExt.authenticate`, so none of them
 * would notice if the protection stopped working — which is exactly why this one exists separately.
 */
class SecurityCsrfTest: TestContext() {
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

	/**
	 * The renewal endpoint runs entirely on the refresh cookie the browser attaches by itself, which
	 * is precisely the shape a CSRF attack takes. It is the one endpoint that must never be exempt.
	 */
	@Test
	fun `Should reject a renewal carrying no CSRF token`() {
		webClient
			.post()
			.uri(uriBuilder("$BASE_URL/token/refresh", emptyList(), emptyList()))
			.cookie(REFRESH_TOKEN_COOKIE, "refreshToken")
			.exchange()
			.expectStatus().isEqualTo(FORBIDDEN.value())
	}

	/**
	 * Opening a session cannot be a CSRF target: there is no ambient credential yet, and the
	 * authorization code is the thing being presented. Requiring a token here would only mean the
	 * frontend had to fetch one before it could log in.
	 */
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

	/**
	 * A caller that sets its own `Authorization` header — Swagger, a service account — is not a CSRF
	 * victim: a browser never attaches that header to a cross-site request. The exemption holds only
	 * because token extraction reads the header before the cookie; otherwise a meaningless header
	 * would buy the exemption while the ambient cookie did the authenticating.
	 */
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

	/**
	 * Echoes the cookie back verbatim, the way the frontend interceptor will.
	 *
	 * This is the only test here that exercises the actual comparison: everywhere else the `csrf()`
	 * mutator supplies a token that matches by construction. Spring defaults to the XOR request
	 * handler, which expects a masked value while the cookie carries the raw one, so with that
	 * default every mutating call from the browser is refused — and no contract test notices,
	 * because none of them ever compares a real cookie against a real header.
	 */
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
