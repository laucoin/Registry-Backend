package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CACHE_CONTROL
import org.springframework.test.web.reactive.server.WebTestClient

/**
 * The response headers the browser is told to enforce.
 *
 * Asserted through the filter chain rather than on the configuration, because what matters is the
 * header that actually reaches the response — a writer registered but never invoked would satisfy
 * any assertion made on the DSL alone.
 */
class SecurityHeadersTest: TestContext() {
	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val CSP = "Content-Security-Policy"
		private const val API_PATH = "/api/v1/users/current"
		private const val ACTUATOR_PATH = "/actuator/health"
	}

	/** The API answers JSON, so it is allowed to load nothing and to be framed by nobody. */
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

	/**
	 * The Swagger UI shares this chain, and it calls two origins this header cannot know: the API on
	 * its own port, and the provider's token endpoint. Applying the API policy here leaves the page
	 * rendering but silently unable to send a request, which is the failure this split exists to
	 * prevent — so the management paths carry framing protection alone.
	 */
	@Test
	fun `Should serve the management paths without constraining what they may call`() {
		webClient
			.get()
			.uri(ACTUATOR_PATH)
			.exchange()
			.expectHeader()
			.valueEquals(CSP, "frame-ancestors 'none'")
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

	/** Guards against redeclaring what Spring already writes, which would produce two sources of truth. */
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
