package fr.laucoin.registry.backend.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN
import org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

/**
 * The CORS contract the browser depends on.
 *
 * Asserted on the configuration rather than on a preflight exchange: `WebTestClient` binds straight
 * to the application and never crosses an origin, so a negotiation test there proves nothing about
 * what a browser would be told. The negotiation itself is checked against a running backend.
 */
class SecurityConfigTest {
	private companion object {
		private const val ORIGIN = "https://registry.test.com"
		private const val CSRF_HEADER = "X-XSRF-TOKEN"
	}

	private val config = SecurityConfig(
		tokenConverter = mock(),
		authorizationErrorHandler = mock(),
		headersHandler = mock(),
		csrfTokenHandler = mock(),
		tokenExtractionHandler = mock(),
		cookieDomain = "registry.test.com",
		cookieSecure = true,
		cookieSameSite = "Lax",
		corsUrls = listOf(ORIGIN),
		documentationEnabled = false,
		observabilityEnabled = false,
	)

	private fun corsConfiguration() = config.corsConfigurationSource()
		.getCorsConfiguration(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/projects")))!!

	/**
	 * Without credentials the browser attaches no cookie at all to a cross-origin call, and every
	 * authenticated request from the SPA fails. It also makes a wildcard origin illegal, which is a
	 * useful guard on whatever `external.cors.urls` is set to.
	 */
	@Test
	fun `Should allow credentials so the session cookies are sent`() {
		assertEquals(true, corsConfiguration().allowCredentials)
	}

	@Test
	fun `Should allow the CSRF header the frontend echoes back`() {
		assertTrue(
			corsConfiguration().allowedHeaders.orEmpty().any { it.equals(CSRF_HEADER, ignoreCase = true) },
			"without it the browser blocks every mutating call before sending it",
		)
	}

	@Test
	fun `Should keep allowing the Authorization header for non-browser callers`() {
		assertTrue(corsConfiguration().allowedHeaders.orEmpty().contains(AUTHORIZATION))
	}

	/**
	 * These three are *response* headers. Listing them as permitted *request* headers never did
	 * anything — a browser does not send them — and reading the list as though it did is what makes
	 * a CORS configuration hard to reason about later.
	 */
	@Test
	fun `Should not list response headers among the accepted request headers`() {
		val allowed = corsConfiguration().allowedHeaders.orEmpty()

		listOf(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_EXPOSE_HEADERS)
			.forEach { assertFalse(allowed.contains(it), "$it is a response header, not a request one") }
	}

	@Test
	fun `Should restrict the origins to the configured allowlist`() {
		assertEquals(listOf(ORIGIN), corsConfiguration().allowedOrigins)
	}
}
