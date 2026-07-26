package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader.AlertStatusReaderDtoMapper
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.authenticate
import fr.laucoin.registry.backend.test.WebTestClientExt.uriBuilder
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.ACCEPT_LANGUAGE
import org.springframework.http.HttpHeaders.IF_NONE_MATCH
import org.springframework.http.HttpHeaders.ORIGIN
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.test.assertNotNull

/**
 * ADR 018 §4 — metadata GETs carry ETag/Cache-Control/Vary and revalidate to
 * 304 without a body.
 */
class CacheHeadersHandlerTest : TestContext() {
	@MockitoBean
	private lateinit var alertStatusReaderMapper: AlertStatusReaderDtoMapper

	@Autowired
	private lateinit var webClient: WebTestClient

	private companion object {
		private const val METADATA_URL = "/api/v2/metadata/movements/types"
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

	@Test
	fun `Should not attach revalidation headers to an error response`() {
		// Arrange
		whenever(alertStatusReaderMapper.toDto(any())).thenThrow(IllegalStateException("boom"))

		// Act + Assert
		webClient
			.authenticate()
			.get()
			.uri(uriBuilder("/api/v2/metadata/alerts/status", emptyList(), emptyList()))
			.exchange()
			.expectStatus().is5xxServerError
			.expectHeader().doesNotExist("ETag")
			.expectHeader().value("Cache-Control") { assert(!it.contains("private")) }
	}
}
