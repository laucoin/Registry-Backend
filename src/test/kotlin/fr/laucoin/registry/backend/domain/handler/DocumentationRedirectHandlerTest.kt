package fr.laucoin.registry.backend.domain.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus.FOUND
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.net.InetSocketAddress

class DocumentationRedirectHandlerTest {
	private companion object {
		private const val API_PORT = 8081
		private const val MANAGEMENT_PORT = 8082
		private const val SWAGGER_UI = "/swagger-ui/index.html"
	}

	private val chain: WebFilterChain = mock<WebFilterChain>().apply {
		whenever(filter(org.mockito.kotlin.any())).thenReturn(Mono.empty())
	}

	private fun handler(documentationEnabled: Boolean = true) =
		DocumentationRedirectHandler(MANAGEMENT_PORT, documentationEnabled)

	private fun exchange(path: String, port: Int) = MockServerWebExchange.from(
		MockServerHttpRequest.get(path).localAddress(InetSocketAddress("localhost", port)),
	)

	private fun locationOf(exchange: MockServerWebExchange) = exchange.response.headers.location?.toString()

	@Test
	fun `Should send the management root to the Swagger UI`() {
		// Arrange
		val exchange = exchange("/", MANAGEMENT_PORT)

		// Act
		handler().filter(exchange, chain).block()

		// Assert
		assertEquals(FOUND, exchange.response.statusCode)
		assertEquals(SWAGGER_UI, locationOf(exchange))
	}

	@Test
	fun `Should leave the management root alone when documentation is disabled`() {
		// Arrange
		val exchange = exchange("/", MANAGEMENT_PORT)

		// Act
		handler(documentationEnabled = false).filter(exchange, chain).block()

		// Assert
		assertNull(locationOf(exchange))
	}

	@Test
	fun `Should leave every other management path alone`() {
		// Arrange
		val exchange = exchange("/health", MANAGEMENT_PORT)

		// Act
		handler().filter(exchange, chain).block()

		// Assert
		assertNull(locationOf(exchange))
	}

	@Test
	fun `Should leave the API port alone`() {
		// Arrange
		val exchange = exchange("/", API_PORT)

		// Act
		handler().filter(exchange, chain).block()

		// Assert
		assertNull(locationOf(exchange))
	}
}
