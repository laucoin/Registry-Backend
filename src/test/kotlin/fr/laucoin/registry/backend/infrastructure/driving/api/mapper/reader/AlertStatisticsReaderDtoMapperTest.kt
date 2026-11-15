package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class AlertStatisticsReaderDtoMapperTest {
	private val mapper = AlertStatisticsReaderDtoMapperImpl()

	@Test
	fun `Should expose the counts, the average and the real alert rate`() {
		// Arrange
		val model = AlertStatisticsModel(resolvedCount = 3, canceledCount = 1, averageResolutionSeconds = 90)

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(3, dto.resolvedCount)
		assertEquals(1, dto.canceledCount)
		assertEquals(90, dto.averageResolutionSeconds)
		assertEquals(0.75, dto.realAlertRate)
	}

	@Test
	fun `Should expose empty values when there is no closed alert`() {
		// Arrange
		val model = AlertStatisticsModel()

		// Act
		val dto = mapper.toDto(model)

		// Assert
		assertEquals(0, dto.resolvedCount)
		assertNull(dto.averageResolutionSeconds)
		assertNull(dto.realAlertRate)
	}
}
