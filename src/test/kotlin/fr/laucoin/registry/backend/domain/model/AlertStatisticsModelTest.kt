package fr.laucoin.registry.backend.domain.model

import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class AlertStatisticsModelTest {
	private companion object {
		@JvmStatic
		fun `Should compute the real alert rate`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(13L, 2L, 13.0 / 15.0),
				Arguments.of(1L, 0L, 1.0),
				Arguments.of(0L, 4L, 0.0),
				Arguments.of(3L, 3L, 0.5),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should compute the real alert rate`(resolved: Long, canceled: Long, expected: Double) {
		// Arrange
		val statistics = AlertStatisticsModel(resolvedCount = resolved, canceledCount = canceled)

		// Act
		val rate = statistics.realAlertRate

		// Assert
		assertEquals(expected, rate)
	}

	@Test
	fun `Should have no real alert rate when no alert is closed`() {
		// Arrange
		val statistics = AlertStatisticsModel()

		// Act
		val rate = statistics.realAlertRate

		// Assert
		assertNull(rate)
	}
}
