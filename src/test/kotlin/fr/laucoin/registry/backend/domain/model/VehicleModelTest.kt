package fr.laucoin.registry.backend.domain.model

import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class VehicleModelTest {
	private fun date(day: Int) = CustomDateTimeModel(LocalDate.of(2026, 7, day))

	@Test
	fun `Should use the availability window of the vehicle, the end date being included`() {
		// Arrange
		val vehicle = VehicleModel(startAvailability = date(3), endAvailability = date(5))

		// Act
		val window = vehicle.usageWindow(ProjectModel(begin = date(1), end = date(31)))

		// Assert
		assertEquals(ZonedDateTime.of(2026, 7, 3, 0, 0, 0, 0, UTC).toInstant(), window!!.start.toInstant())
		assertEquals(LocalDate.of(2026, 7, 5), window.end.toLocalDate())
		assertEquals(23, window.end.hour)
	}

	@Test
	fun `Should take a missing bound from the project dates`() {
		// Arrange
		val vehicle = VehicleModel(startAvailability = date(3))

		// Act
		val window = vehicle.usageWindow(ProjectModel(begin = date(1), end = date(10)))

		// Assert
		assertEquals(LocalDate.of(2026, 7, 3), window!!.start.toLocalDate())
		assertEquals(LocalDate.of(2026, 7, 10), window.end.toLocalDate())
	}

	@Test
	fun `Should use the project dates when the vehicle has no availability window`() {
		// Arrange
		val vehicle = VehicleModel()

		// Act
		val window = vehicle.usageWindow(ProjectModel(begin = date(1), end = date(10)))

		// Assert
		assertEquals(LocalDate.of(2026, 7, 1), window!!.start.toLocalDate())
		assertEquals(LocalDate.of(2026, 7, 10), window.end.toLocalDate())
	}

	@Test
	fun `Should have no window when a bound is missing everywhere`() {
		// Arrange
		val vehicle = VehicleModel(startAvailability = date(3))

		// Act
		val window = vehicle.usageWindow(ProjectModel(begin = date(1)))

		// Assert
		assertNull(window)
	}

	@Test
	fun `Should have no window when the end is not after the start`() {
		// Arrange
		val vehicle = VehicleModel(startAvailability = date(5), endAvailability = date(3))

		// Act
		val window = vehicle.usageWindow(ProjectModel())

		// Assert
		assertNull(window)
	}
}
