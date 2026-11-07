package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class ActivityStatisticsReaderDtoMapperTest {
	private val mapper = ActivityStatisticsReaderDtoMapperImpl()

	@Test
	fun `Should expose the counts, the rate and the durations`() {
		// Arrange
		val model = ActivityStatisticsModel(
			participantCount = 3,
			projectParticipantCount = 12,
			movementSeconds = 3_600,
			manSeconds = 10_800,
		)

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(3, dto.participantCount)
		assertEquals(12, dto.projectParticipantCount)
		assertEquals(0.25, dto.participantRate)
		assertEquals(3_600, dto.movementSeconds)
		assertEquals(10_800, dto.manSeconds)
	}

	@Test
	fun `Should expose no rate when the project has no participant`() {
		// Arrange
		val model = ActivityStatisticsModel()

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertNull(dto.participantRate)
	}
}
