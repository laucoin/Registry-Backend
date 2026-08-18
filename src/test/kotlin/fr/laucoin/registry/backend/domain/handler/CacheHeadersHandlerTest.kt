package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.annotation.HttpCacheable
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.IF_NONE_MATCH
import org.springframework.http.HttpHeaders.ORIGIN
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.pattern.PathPatternParser
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CacheHeadersHandlerTest : TestContext() {
	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val METADATA_URL = "/api/v2/metadata/features"
	}

	@Test
	fun `Should serve metadata with revalidation headers and honour If-None-Match`() {
		// Act
		val result = webClient
			.authenticate()
			.get()
			.uri(uriBuilder(METADATA_URL, emptyList(), emptyList()))
			.exchange()
			.expectStatus().isOk
			.expectHeader().exists("ETag")
			.expectHeader().valueEquals("Cache-Control", "private, max-age=300")
			.expectHeader().value("Vary") { assert(it.contains(ACCEPT_LANGUAGE)) }
			.returnResult(String::class.java)
		val etag = result.responseHeaders.eTag
		assertNotNull(etag)

		// Act + Assert
		webClient
			.authenticate()
			.get()
			.uri(uriBuilder(METADATA_URL, emptyList(), emptyList()))
			.header(IF_NONE_MATCH, etag)
			.exchange()
			.expectStatus().isNotModified
	}

	@Test
	fun `Should keep locale variants apart through the ETag`() {
		// Act
		val english = webClient.authenticate().get()
			.uri(uriBuilder(METADATA_URL, emptyList(), emptyList()))
			.header(ACCEPT_LANGUAGE, "en")
			.exchange().returnResult(String::class.java).responseHeaders.eTag
		val french = webClient.authenticate().get()
			.uri(uriBuilder(METADATA_URL, emptyList(), emptyList()))
			.header(ACCEPT_LANGUAGE, "fr")
			.exchange().returnResult(String::class.java).responseHeaders.eTag

		// Assert
		assertNotNull(english)
		assertNotNull(french)
		assert(english != french)
	}

	@Test
	fun `Should leave non-metadata endpoints untouched`() {
		// Act + Assert
		webClient
			.authenticate()
			.get()
			.uri(uriBuilder("/api/v2/users/assignable-roles", emptyList(), emptyList()))
			.exchange()
			.expectHeader().doesNotExist("ETag")
	}

	/**
	 * The CORS processor stamps its Vary values on every matching response,
	 * even without an Origin header (which the mock server cannot carry —
	 * reactive CorsUtils.isSameOrigin requires an absolute request URI).
	 */
	@Test
	fun `Should append to Vary without clobbering the CORS values`() {
		// Act + Assert
		webClient
			.authenticate()
			.get()
			.uri(uriBuilder(METADATA_URL, emptyList(), emptyList()))
			.exchange()
			.expectStatus().isOk
			.expectHeader().value("Vary") {
				assert(it.contains(ORIGIN))
				assert(it.contains(ACCEPT_LANGUAGE))
			}
	}

	/**
	 * The 5xx branch is asserted directly on the filter: the only remaining
	 * cacheable endpoint returns a config switch and cannot be driven into an
	 * error through the HTTP layer. A still-unset status at commit means the
	 * implied 200, so only an explicit non-2xx must suppress the validator.
	 */
	@Test
	fun `Should not attach revalidation headers to an error response`() {
		// Arrange
		val annotatedEndpoints: AnnotatedEndpointsHandler = mock()
		val parser = PathPatternParser()
		val endpoint = AnnotatedEndpointsHandler.AnnotatedEndpoint(
			setOf(parser.parse(METADATA_URL)),
			setOf(GET),
			HttpCacheable(),
		)
		whenever(annotatedEndpoints.endpoints(HttpCacheable::class.java)).thenReturn(listOf(endpoint))
		val handler = CacheHeadersHandler(300, annotatedEndpoints)
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get(METADATA_URL))

		// Act
		handler.filter(exchange) {
			exchange.response.statusCode = INTERNAL_SERVER_ERROR
			exchange.response.setComplete()
		}.block()

		// Assert
		assertNull(exchange.response.headers.eTag)
		assertNull(exchange.response.headers.cacheControl)
	}
}
