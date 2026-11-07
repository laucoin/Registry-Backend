package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.expectedLastEditions
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.ETagExt.toETag
import java.time.Instant
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpHeaders.IF_MATCH
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class ETagExtTest {
	private companion object {
		private val edition: Instant = Instant.parse("2026-10-13T10:02:11.123456Z")
		private val currentETag = edition.toETag()
		private val editionMillis: Instant = Instant.ofEpochMilli(edition.toEpochMilli())

		@JvmStatic
		fun `Should expose no precondition without a usable If-Match`(): Stream<Arguments> = Stream.of(
			Arguments.of(null),
			Arguments.of("*"),
			Arguments.of("\"1\", *"),
		)

		@JvmStatic
		fun `Should expose the expected last editions`(): Stream<Arguments> = Stream.of(
			Arguments.of(currentETag, listOf(editionMillis)),
			Arguments.of("W/$currentETag", listOf(editionMillis)),
			Arguments.of("\"1\", $currentETag", listOf(Instant.ofEpochMilli(1), editionMillis)),
			Arguments.of("\"not-a-number\"", emptyList<Instant>()),
		)
	}

	private fun exchange(ifMatch: String?) = MockServerWebExchange.from(
		MockServerHttpRequest.patch("/api/v2/anything").also { builder -> ifMatch?.let { builder.header(IF_MATCH, it) } },
	)

	@Test
	fun `Should format the ETag with the millisecond of the instant`() {
		// Act
		val result = Instant.parse("2026-10-13T10:02:11.123456Z").toETag()

		// Assert
		assertEquals("\"1791885731123\"", result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should expose no precondition without a usable If-Match`(ifMatch: String?) {
		// Act
		val result = exchange(ifMatch).expectedLastEditions()

		// Assert
		assertNull(result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should expose the expected last editions`(ifMatch: String, expected: List<Instant>) {
		// Act
		val result = exchange(ifMatch).expectedLastEditions()

		// Assert
		assertEquals(expected, result)
	}
}
