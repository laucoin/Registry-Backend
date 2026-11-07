package fr.laucoin.registry.backend.domain.model

import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class TimelineWindowModelTest {
	private val base = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, UTC)
	private val window = TimelineWindowModel(base.plusHours(10), base.plusHours(20))

	@Test
	fun `Should give the length of the window in seconds`() {
		// Arrange

		// Act
		val seconds = window.seconds

		// Assert
		assertEquals(10 * 3600L, seconds)
	}

	@Test
	fun `Should count only the part of an outing inside the window`() {
		// Arrange
		val outing = OutingModel(base.plusHours(8), base.plusHours(12))

		// Act
		val seconds = window.overlapSeconds(outing)

		// Assert
		assertEquals(2 * 3600L, seconds)
	}

	@Test
	fun `Should count nothing for an outing outside the window`() {
		// Arrange
		val outing = OutingModel(base.plusHours(21), base.plusHours(25))

		// Act
		val seconds = window.overlapSeconds(outing)

		// Assert
		assertEquals(0, seconds)
	}
}
