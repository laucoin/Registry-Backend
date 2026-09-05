package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.handler.AuthenticationCookieHandler.Companion.ACCESS_TOKEN_COOKIE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import reactor.test.StepVerifier

class TokenExtractionHandlerTest {
	private companion object {
		private const val HEADER_TOKEN = "token-from-the-header"
		private const val COOKIE_TOKEN = "token-from-the-cookie"

		private fun exchange(header: String? = null, cookie: String? = null): MockServerWebExchange {
			var request = MockServerHttpRequest.get("/api/v1/projects")
			header?.let { request = request.header(AUTHORIZATION, "Bearer $it") }
			cookie?.let { request = request.cookie(HttpCookie(ACCESS_TOKEN_COOKIE, it)) }
			return MockServerWebExchange.from(request)
		}
	}

	private val handler = TokenExtractionHandler()

	@Test
	fun `Should read the token from the session cookie`() {
		StepVerifier.create(handler.convert(exchange(cookie = COOKIE_TOKEN)))
			.assertNext { assertEquals(COOKIE_TOKEN, (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	@Test
	fun `Should read the token from the Authorization header`() {
		StepVerifier.create(handler.convert(exchange(header = HEADER_TOKEN)))
			.assertNext { assertEquals(HEADER_TOKEN, (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	/**
	 * The header wins, and this is the test that matters most here.
	 *
	 * A header is set on purpose; a cookie is attached by the browser regardless of intent. Two
	 * things depend on this ordering. Swagger's *Authorize* would otherwise be silently overridden
	 * by whatever session the developer has open in the same browser. More importantly, the CSRF
	 * exemption planned for the next step skips CSRF whenever an `Authorization` header is present,
	 * on the grounds that such a caller cannot be a CSRF victim — were the cookie to win, a request
	 * could carry a meaningless header to buy that exemption while still authenticating through the
	 * ambient cookie.
	 */
	@Test
	fun `Should prefer the header over the cookie when both are present`() {
		StepVerifier.create(handler.convert(exchange(header = HEADER_TOKEN, cookie = COOKIE_TOKEN)))
			.assertNext { assertEquals(HEADER_TOKEN, (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}

	@Test
	fun `Should extract nothing when neither is present`() {
		StepVerifier.create(handler.convert(exchange())).verifyComplete()
	}

	/**
	 * An empty cookie is what a browser sends just after a logout cleared it, and it must not be
	 * mistaken for a credential: an empty bearer token would fail decoding with a confusing error
	 * instead of a plain "not authenticated".
	 */
	@Test
	fun `Should ignore a blank cookie rather than treat it as a credential`() {
		StepVerifier.create(handler.convert(exchange(cookie = ""))).verifyComplete()
	}

	@Test
	fun `Should fall back to the cookie when the header carries no bearer token`() {
		val request = MockServerHttpRequest.get("/api/v1/projects")
			.header(AUTHORIZATION, "Basic dXNlcjpwYXNz")
			.cookie(HttpCookie(ACCESS_TOKEN_COOKIE, COOKIE_TOKEN))
		StepVerifier.create(handler.convert(MockServerWebExchange.from(request)))
			.assertNext { assertEquals(COOKIE_TOKEN, (it as BearerTokenAuthenticationToken).token) }
			.verifyComplete()
	}
}
