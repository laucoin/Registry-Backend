package fr.laucoin.registry.backend.domain.model

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
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

	@Test
	fun `Should plusHours move to the next day when the shift crosses midnight`() {
		// Arrange
		val start = CustomDateTimeModel(LocalDate.of(2030, 1, 1), OffsetTime.of(23, 30, 0, 0, ZoneOffset.UTC))

		// Act
		val result = start.plusHours(1)

		// Assert
		assertEquals(CustomDateTimeModel(LocalDate.of(2030, 1, 2), OffsetTime.of(0, 30, 0, 0, ZoneOffset.UTC)), result)
	}

	@Test
	fun `Should plusHours leave the original value untouched`() {
		// Arrange
		val start = CustomDateTimeModel(LocalDate.of(2030, 1, 1), OffsetTime.of(10, 0, 0, 0, ZoneOffset.UTC))

		// Act
		start.plusHours(1)

		// Assert
		assertEquals(CustomDateTimeModel(LocalDate.of(2030, 1, 1), OffsetTime.of(10, 0, 0, 0, ZoneOffset.UTC)), start)
	}

	@Test
	fun `Should plusHours keep a date without time unchanged`() {
		// Arrange
		val start = CustomDateTimeModel(LocalDate.of(2030, 1, 1))

		// Act
		val result = start.plusHours(1)

		// Assert
		assertEquals(CustomDateTimeModel(LocalDate.of(2030, 1, 1)), result)
	}
}
