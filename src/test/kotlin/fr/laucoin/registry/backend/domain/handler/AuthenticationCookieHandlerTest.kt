package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.ACCESS_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.REFRESH_TOKEN_COOKIE
import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.REFRESH_TOKEN_PATH
import fr.laucoin.registry.backend.domain.model.TokenModel
import java.time.Duration
import kotlin.test.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseCookie
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class AuthenticationCookieHandlerTest {
	private companion object {
		private const val DOMAIN = "registry.laucoin.fr"
		private const val ACCESS_TOKEN = "an-access-token"
		private const val REFRESH_TOKEN = "a-refresh-token"

		private val token = TokenModel(
			accessToken = ACCESS_TOKEN,
			expiresIn = 300,
			refreshExpiresIn = 3600,
			refreshToken = REFRESH_TOKEN,
			tokenType = "Bearer",
		)

		private fun handler(domain: String = DOMAIN, secure: Boolean = true, sameSite: String = "Lax") =
			AuthenticationCookieHandler(domain, secure, sameSite)

		private fun exchange() = MockServerWebExchange.from(MockServerHttpRequest.get("/"))
	}

	private fun MockServerWebExchange.cookie(name: String): ResponseCookie =
		response.cookies.getFirst(name) ?: error("Cookie \"$name\" was not written")

	@Test
	fun `Should write the access token beyond the reach of any script`() {
		val exchange = exchange()

		handler().write(exchange, token)

		val cookie = exchange.cookie(ACCESS_TOKEN_COOKIE)
		assertEquals(ACCESS_TOKEN, cookie.value)
		assertTrue(cookie.isHttpOnly)
		assertTrue(cookie.isSecure)
		assertEquals("Lax", cookie.sameSite)
		assertEquals("/", cookie.path)
		assertEquals(DOMAIN, cookie.domain)
		assertEquals(Duration.ofSeconds(300), cookie.maxAge)
	}

	/**
	 * The refresh token is the renewable credential: it is confined to the refresh path so it never
	 * travels with ordinary API traffic, and pinned to `Strict` whatever the access cookie does.
	 */
	@Test
	fun `Should confine the refresh token to the refresh path`() {
		val exchange = exchange()

		handler(sameSite = "Lax").write(exchange, token)

		val cookie = exchange.cookie(REFRESH_TOKEN_COOKIE)
		assertEquals(REFRESH_TOKEN, cookie.value)
		assertTrue(cookie.isHttpOnly)
		assertEquals(REFRESH_TOKEN_PATH, cookie.path)
		assertEquals("Strict", cookie.sameSite)
		assertEquals(Duration.ofSeconds(3600), cookie.maxAge)
	}

	/**
	 * Local development runs without TLS, so `Secure` has to be configurable — a `Secure` cookie
	 * over plain http is simply dropped, and every sign-in would silently fail.
	 */
	/**
	 * Authentik issues no refresh token unless `offline_access` was requested. Writing an empty
	 * cookie would look like a session that can be renewed and fail confusingly on the first
	 * attempt; leaving it out makes the session simply end when the access token expires.
	 */
	@Test
	fun `Should write no refresh cookie when the provider issued no refresh token`() {
		val exchange = exchange()

		handler().write(exchange, token.copy(refreshToken = null, refreshExpiresIn = null))

		assertEquals(ACCESS_TOKEN, exchange.cookie(ACCESS_TOKEN_COOKIE).value)
		assertNull(exchange.response.cookies.getFirst(REFRESH_TOKEN_COOKIE))
	}

	/**
	 * A provider that issues a refresh token without stating its lifetime gets a session cookie —
	 * dropped when the browser closes, which is exactly what `sessionStorage` did before this
	 * change. Inventing an expiry the provider never stated would be worse.
	 */
	@Test
	fun `Should write a session cookie when the refresh lifetime is unknown`() {
		val exchange = exchange()

		handler().write(exchange, token.copy(refreshExpiresIn = null))

		assertEquals(Duration.ofSeconds(-1), exchange.cookie(REFRESH_TOKEN_COOKIE).maxAge)
	}

	@Test
	fun `Should allow an insecure cookie for local development`() {
		val exchange = exchange()

		handler(secure = false).write(exchange, token)

		assertFalse(exchange.cookie(ACCESS_TOKEN_COOKIE).isSecure)
	}

	/**
	 * An empty domain must leave the attribute off rather than emit `Domain=`, which browsers
	 * reject. The cookie is then host-only, which is what `localhost` needs.
	 */
	@Test
	fun `Should omit the domain attribute when none is configured`() {
		val exchange = exchange()

		handler(domain = "").write(exchange, token)

		assertNull(exchange.cookie(ACCESS_TOKEN_COOKIE).domain)
		assertNull(exchange.cookie(REFRESH_TOKEN_COOKIE).domain)
	}

	/**
	 * A browser only drops a cookie when the replacement matches its name, domain and path, so the
	 * cleared cookies must mirror the written ones exactly.
	 */
	@Test
	fun `Should expire both cookies on the same paths they were written to`() {
		val exchange = exchange()

		handler().clear(exchange)

		val access = exchange.cookie(ACCESS_TOKEN_COOKIE)
		val refresh = exchange.cookie(REFRESH_TOKEN_COOKIE)
		assertEquals(Duration.ZERO, access.maxAge)
		assertEquals(Duration.ZERO, refresh.maxAge)
		assertEquals("", access.value)
		assertEquals("", refresh.value)
		assertEquals("/", access.path)
		assertEquals(REFRESH_TOKEN_PATH, refresh.path)
		assertEquals(DOMAIN, access.domain)
		assertEquals(DOMAIN, refresh.domain)
	}
}
