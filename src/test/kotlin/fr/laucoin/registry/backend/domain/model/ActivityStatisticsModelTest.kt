package fr.laucoin.registry.backend.domain.model

import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class ActivityStatisticsModelTest {
	private companion object {
		@JvmStatic
		fun `Should compute the participant rate`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(5L, 20L, 0.25),
				Arguments.of(0L, 20L, 0.0),
				Arguments.of(20L, 20L, 1.0),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should compute the participant rate`(participants: Long, total: Long, expected: Double) {
		// Arrange
		val statistics = ActivityStatisticsModel(participantCount = participants, projectParticipantCount = total)

		// Act
		val rate = statistics.participantRate

		// Assert
		assertEquals(expected, rate)
	}

	@Test
	fun `Should have no participant rate when the project has no participant`() {
		// Arrange
		val statistics = ActivityStatisticsModel(participantCount = 0, projectParticipantCount = 0)

		// Act
		val rate = statistics.participantRate

		// Assert
		assertNull(rate)
	}
}
