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
import java.net.InetSocketAddress

class SecurityConfigTest {
	private companion object {
		private const val ORIGIN = "https://registry.test.com"
		private const val CSRF_HEADER = "X-XSRF-TOKEN"
		private const val API_CSP = "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"
		private const val DOCUMENTATION_CSP = "frame-ancestors 'none'"
	}

	private val config = SecurityConfig(
		tokenConverter = mock(),
		authorizationErrorHandler = mock(),
		headersHandler = mock(),
		csrfTokenHandler = mock(),
		tokenExtractionHandler = mock(),
		documentationRedirectHandler = mock(),
		cookieDomain = "registry.test.com",
		cookieSecure = true,
		cookieSameSite = "Lax",
		corsUrls = listOf(ORIGIN),
		managementPort = 8082,
		documentationEnabled = false,
		observabilityEnabled = false,
	)

	private fun corsConfiguration() = config.corsConfigurationSource()
		.getCorsConfiguration(MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/projects")))!!

	@Test
	fun `Should allow credentials so the session cookies are sent`() {
		// Assert
		assertEquals(true, corsConfiguration().allowCredentials)
	}

	@Test
	fun `Should allow the CSRF header the frontend echoes back`() {
		// Assert
		assertTrue(
			corsConfiguration().allowedHeaders.orEmpty().any { it.equals(CSRF_HEADER, ignoreCase = true) },
			"without it the browser blocks every mutating call before sending it",
		)
	}

	@Test
	fun `Should keep allowing the Authorization header for non-browser callers`() {
		// Assert
		assertTrue(corsConfiguration().allowedHeaders.orEmpty().contains(AUTHORIZATION))
	}

	@Test
	fun `Should not list response headers among the accepted request headers`() {
		// Act
		val allowed = corsConfiguration().allowedHeaders.orEmpty()

		// Assert
		listOf(ACCESS_CONTROL_ALLOW_ORIGIN, ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_EXPOSE_HEADERS)
			.forEach { assertFalse(allowed.contains(it), "$it is a response header, not a request one") }
	}

	@Test
	fun `Should restrict the origins to the configured allowlist`() {
		// Assert
		assertEquals(listOf(ORIGIN), corsConfiguration().allowedOrigins)
	}

	@Test
	fun `Should serve the API policy on the API port and the documentation policy on the management one`() {
		// Assert
		assertEquals(API_CSP, policyServedOn(port = 8081))
		assertEquals(DOCUMENTATION_CSP, policyServedOn(port = 8082))
	}

	@Test
	fun `Should permit only what arrives on the management port`() {
		// Assert
		assertFalse(config.onManagementPort().matches(exchangeOn(port = 8081)).block()!!.isMatch)
		assertTrue(config.onManagementPort().matches(exchangeOn(port = 8082)).block()!!.isMatch)
	}

	private fun exchangeOn(port: Int) = MockServerWebExchange.from(
		MockServerHttpRequest.get("/anything").localAddress(InetSocketAddress("localhost", port)),
	)

	private fun policyServedOn(port: Int): String {
		// Act
		val exchange = exchangeOn(port)
		config.contentSecurityPolicyWriter().writeHttpHeaders(exchange).block()

		// Assert
		return exchange.response.headers.getFirst("Content-Security-Policy")!!
	}
}
