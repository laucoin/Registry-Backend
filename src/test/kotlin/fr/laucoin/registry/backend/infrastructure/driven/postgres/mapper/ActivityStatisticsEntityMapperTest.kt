package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.ActivityStatisticsEntity
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ActivityStatisticsEntityMapperTest {
	private val mapper = ActivityStatisticsEntityMapper()

	@Test
	fun `Should map the counts and round the durations to seconds`() {
		// Arrange
		val entity = ActivityStatisticsEntity(
			participantCount = 3,
			projectParticipantCount = 12,
			movementSeconds = 3_600.4,
			manSeconds = 7_200.6,
		)

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertEquals(3, model.participantCount)
		assertEquals(12, model.projectParticipantCount)
		assertEquals(3_600, model.movementSeconds)
		assertEquals(7_201, model.manSeconds)
	}

	@Test
	fun `Should default missing values to zero`() {
		// Arrange
		val entity = ActivityStatisticsEntity()

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertEquals(0, model.participantCount)
		assertEquals(0, model.manSeconds)
	}
}
