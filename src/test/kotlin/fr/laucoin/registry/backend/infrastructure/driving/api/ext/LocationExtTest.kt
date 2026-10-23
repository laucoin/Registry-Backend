package fr.laucoin.registry.backend.infrastructure.driving.api.ext

import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.ext.LocationExt.created
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.http.HttpStatus.CREATED
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange

class LocationExtTest {
	private val createdId = UUID.randomUUID()

	@Test
	fun `Should append the identifier to the collection path`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v2/projects/1/activities/"))

		// Act
		val result = exchange.created(ActivityReaderDto().apply { this.id = createdId })

		// Assert
		assertEquals(CREATED, result.statusCode)
		assertEquals("/api/v2/projects/1/activities/$createdId", result.headers.getFirst(LOCATION))
	}

	@Test
	fun `Should drop the collection suffix`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v2/projects/1/movements/guests"))

		// Act
		val result = exchange.created(ActivityReaderDto().apply { this.id = createdId }, "/guests")

		// Assert
		assertEquals("/api/v2/projects/1/movements/$createdId", result.headers.getFirst(LOCATION))
	}

	@Test
	fun `Should omit the location without identifier`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v2/projects/1/activities"))

		// Act
		val result = exchange.created(ActivityReaderDto())

		// Assert
		assertEquals(CREATED, result.statusCode)
		assertNull(result.headers.getFirst(LOCATION))
	}

	@Test
	fun `Should expose the ETag of the created resource`() {
		// Arrange
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v2/projects/1/activities"))
		val body = ActivityReaderDto().apply {
			this.id = createdId
			lastEdition = HistoryModel(dateTime = ZonedDateTime.parse("2026-10-13T10:02:11.123456Z"))
		}

		// Act
		val result = exchange.created(body)

		// Assert
		assertEquals("\"1791885731123\"", result.headers.eTag)
	}
}
