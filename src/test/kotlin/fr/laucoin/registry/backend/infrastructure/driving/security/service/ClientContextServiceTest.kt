package fr.laucoin.registry.backend.infrastructure.driving.security.service

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import java.net.InetSocketAddress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.slf4j.LoggerFactory

class ClientContextServiceTest {
	private fun request(peer: String?, forwardedFor: String? = null, userAgent: String? = null): ServerHttpRequest {
		val builder = MockServerHttpRequest.get("/")
		peer?.let { builder.remoteAddress(InetSocketAddress(it, 12345)) }
		forwardedFor?.let { builder.header("X-Forwarded-For", it) }
		userAgent?.let { builder.header("User-Agent", it) }
		return builder.build()
	}

	private fun service(vararg proxies: String) = ClientContextService(proxies.toList())

	@Test
	fun `Should resolve ignore X-Forwarded-For without trusted proxy`() {
		// Arrange
		val request = request("203.0.113.9", forwardedFor = "198.51.100.1")

		// Act
		val result = service().resolve(request)

		// Assert
		assertEquals("203.0.113.9", result.ip)
	}

	@Test
	fun `Should resolve ignore X-Forwarded-For from an untrusted peer`() {
		// Arrange
		val request = request("203.0.113.9", forwardedFor = "198.51.100.1")

		// Act
		val result = service("10.0.0.0/8").resolve(request)

		// Assert
		assertEquals("203.0.113.9", result.ip)
	}

	@Test
	fun `Should resolve use the first untrusted hop from the right behind a trusted proxy`() {
		// Arrange
		val request = request("10.0.0.2", forwardedFor = "1.1.1.1, 198.51.100.7, 10.0.0.1")

		// Act
		val result = service("10.0.0.0/8").resolve(request)

		// Assert
		assertEquals("198.51.100.7", result.ip)
	}

	@Test
	fun `Should resolve fall back to the leftmost entry when every hop is trusted`() {
		// Arrange
		val request = request("10.0.0.2", forwardedFor = "10.0.0.9, 10.0.0.1")

		// Act
		val result = service("10.0.0.0/8").resolve(request)

		// Assert
		assertEquals("10.0.0.9", result.ip)
	}

	@Test
	fun `Should resolve ignore non literal entries and fall back to the peer`() {
		// Arrange
		val request = request("10.0.0.2", forwardedFor = "evil.example.com, ab.cd")

		// Act
		val result = service("10.0.0.2").resolve(request)

		// Assert
		assertEquals("10.0.0.2", result.ip)
	}

	@Test
	fun `Should resolve accept an IPv6 client behind a trusted proxy`() {
		// Arrange
		val request = request("10.0.0.2", forwardedFor = "2001:db8::1")

		// Act
		val result = service("10.0.0.2").resolve(request)

		// Assert
		assertEquals("2001:db8::1", result.ip)
	}

	@Test
	fun `Should resolve return the user agent and a null IP without remote address`() {
		// Arrange
		val request = request(null, userAgent = "JUnit/1.0")

		// Act
		val result = service("10.0.0.0/8").resolve(request)

		// Assert
		assertNull(result.ip)
		assertEquals("JUnit/1.0", result.userAgent)
	}

	private fun startupMessages(vararg proxies: String): List<String> {
		val logger = LoggerFactory.getLogger(ClientContextService::class.java) as Logger
		val appender = ListAppender<ILoggingEvent>().apply { start() }
		logger.addAppender(appender)
		try {
			service(*proxies)
		} finally {
			logger.detachAppender(appender)
		}
		return appender.list.map { it.formattedMessage }
	}

	@Test
	fun `Should signal at startup that no trusted proxy is configured`() {
		// Act
		val messages = startupMessages()

		// Assert
		assertEquals(listOf("No trusted proxy is configured: the client IP is the connection address"), messages)
	}

	@Test
	fun `Should stay silent at startup when a trusted proxy is configured`() {
		// Act
		val messages = startupMessages("10.0.0.0/8")

		// Assert
		assertTrue(messages.isEmpty())
	}
}
