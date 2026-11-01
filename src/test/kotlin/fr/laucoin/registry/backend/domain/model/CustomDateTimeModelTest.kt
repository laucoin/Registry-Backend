package fr.laucoin.registry.backend.domain.model

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals

class CustomDateTimeModelTest {
	@Test
	fun `Should now read the injected clock in UTC whatever its zone`() {
		// Arrange
		val clock = Clock.fixed(Instant.parse("2030-03-04T23:30:00Z"), ZoneId.of("Asia/Tokyo"))

		// Act
		val result = CustomDateTimeModel.now(clock)

		// Assert
		assertEquals(LocalDate.of(2030, 3, 4), result.date)
		assertEquals(Instant.parse("2030-03-04T23:30:00Z"), result.toZonedDateTime()!!.toInstant())
	}
}
