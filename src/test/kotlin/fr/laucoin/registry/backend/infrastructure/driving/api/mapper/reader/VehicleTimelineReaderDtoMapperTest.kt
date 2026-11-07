package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import fr.laucoin.registry.backend.domain.model.VehicleTimelineModel
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class VehicleTimelineReaderDtoMapperTest {
	private val mapper = VehicleTimelineReaderDtoMapperImpl(TimelineReaderDtoMapperImpl())
	private val start = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, UTC)

	@Test
	fun `Should expose the ranges, the outings, the time out and the usage rate`() {
		// Arrange
		val from = CustomDateTimeModel(LocalDate.of(2026, 7, 1))
		val model = VehicleTimelineModel(
			planned = listOf(TimelineRangeModel(from, null)),
			actual = listOf(OutingModel(start, start.plusHours(2))),
			outSeconds = 7_200,
			usageRate = 0.25,
		)

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(from, dto.planned.single().start)
		assertEquals(start.plusHours(2), dto.actual.single().end)
		assertEquals(7_200, dto.outSeconds)
		assertEquals(0.25, dto.usageRate)
	}

	@Test
	fun `Should expose an empty timeline without usage rate`() {
		// Arrange
		val model = VehicleTimelineModel()

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertTrue(dto.planned.isEmpty())
		assertTrue(dto.actual.isEmpty())
		assertEquals(0, dto.outSeconds)
		assertNull(dto.usageRate)
	}
}
