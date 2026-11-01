package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.ParticipantTimelineModel
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class ParticipantTimelineReaderDtoMapperTest {
	private val mapper = ParticipantTimelineReaderDtoMapper(TimelineReaderDtoMapper())
	private val start = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, UTC)

	@Test
	fun `Should expose the planned ranges and the outings`() {
		// Arrange
		val from = CustomDateTimeModel(LocalDate.of(2026, 7, 1))
		val model = ParticipantTimelineModel(
			planned = listOf(TimelineRangeModel(from, null)),
			actual = listOf(OutingModel(start, start.plusHours(3), ongoing = true)),
		)

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(from, dto.planned.single().start)
		assertNull(dto.planned.single().end)
		assertEquals(start, dto.actual.single().start)
		assertEquals(start.plusHours(3), dto.actual.single().end)
		assertTrue(dto.actual.single().ongoing)
	}

	@Test
	fun `Should expose empty lists for an empty timeline`() {
		// Arrange
		val model = ParticipantTimelineModel()

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertTrue(dto.planned.isEmpty())
		assertTrue(dto.actual.isEmpty())
	}
}
