package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertStatisticsEntity
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class AlertStatisticsEntityMapperTest {
	private val mapper = AlertStatisticsEntityMapper()

	@Test
	fun `Should map counts and round the average resolution to seconds`() {
		// Arrange
		val entity = AlertStatisticsEntity(resolvedCount = 13, canceledCount = 2, averageResolutionSeconds = 7_200.6)

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertEquals(13, model.resolvedCount)
		assertEquals(2, model.canceledCount)
		assertEquals(7_201, model.averageResolutionSeconds)
	}

	@Test
	fun `Should default missing counts to zero and keep a missing average empty`() {
		// Arrange
		val entity = AlertStatisticsEntity()

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertEquals(0, model.resolvedCount)
		assertEquals(0, model.canceledCount)
		assertNull(model.averageResolutionSeconds)
	}
}
