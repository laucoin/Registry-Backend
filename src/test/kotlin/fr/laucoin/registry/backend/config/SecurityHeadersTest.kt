package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.test.web.reactive.server.WebTestClient

class SecurityHeadersTest : TestContext() {
	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val CSP = "Content-Security-Policy"
		private const val API_PATH = "/api/v1/users/current"
	}

	@Test
	fun `Should serve the API with a policy that permits no subresource at all`() {
		webClient
			.authenticate()
			.get()
			.uri(API_PATH)
			.exchange()
			.expectHeader()
			.valueEquals(CSP, "default-src 'none'; frame-ancestors 'none'; base-uri 'none'")
	}

	@Test
	fun `Should refuse framing and restrict what is sent to other origins`() {
		webClient
			.authenticate()
			.get()
			.uri(API_PATH)
			.exchange()
			.expectHeader()
			.valueEquals("X-Frame-Options", "DENY")
			.expectHeader()
			.valueEquals("Referrer-Policy", "strict-origin-when-cross-origin")
			.expectHeader()
			.valueEquals("Permissions-Policy", "camera=(), microphone=(), geolocation=(), payment=()")
	}

	@Test
	fun `Should keep the defaults Spring already writes`() {
		webClient
			.authenticate()
			.get()
			.uri(API_PATH)
			.exchange()
			.expectHeader()
			.valueEquals("X-Content-Type-Options", "nosniff")
			.expectHeader()
			.valueEquals(CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate")
	}
}
