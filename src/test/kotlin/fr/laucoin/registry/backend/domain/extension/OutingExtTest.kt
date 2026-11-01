package fr.laucoin.registry.backend.domain.extension

import org.junit.jupiter.api.Test
import fr.laucoin.registry.backend.domain.model.TimelineWindowModel
import fr.laucoin.registry.backend.domain.extension.OutingExt.usageRate
import fr.laucoin.registry.backend.domain.extension.OutingExt.outSeconds
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.extension.OutingExt.toOutings
import fr.laucoin.registry.backend.domain.model.MovementEventModel
import fr.laucoin.registry.backend.domain.model.OutingModel
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class OutingExtTest {
	private companion object {
		val base: ZonedDateTime = ZonedDateTime.of(2026, 7, 1, 0, 0, 0, 0, UTC)
		val now: ZonedDateTime = base.plusHours(100)

		fun at(hour: Long) = base.plusHours(hour)
		fun event(hour: Long, type: MovementTypeEnum, reason: MovementReasonEnum? = null) =
			MovementEventModel(at(hour), type, reason)

		fun closed(from: Long, to: Long) = OutingModel(at(from), at(to))
		fun ongoing(from: Long) = OutingModel(at(from), now, ongoing = true)

		@JvmStatic
		fun `Should compute the usage rate`(): Stream<Arguments> {
			val window = TimelineWindowModel(at(0), at(10))
			return Stream.of(
				Arguments.of(window, listOf(closed(2, 4)), 0.2),
				Arguments.of(window, emptyList<OutingModel>(), 0.0),
				Arguments.of(window, listOf(closed(-5, 15)), 1.0),
				Arguments.of(null, listOf(closed(2, 4)), null),
				Arguments.of(TimelineWindowModel(at(3), at(3)), listOf(closed(2, 4)), null),
			)
		}

		@JvmStatic
		fun `Should deduce the outings from the movements`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(emptyList<MovementEventModel>(), emptyList<OutingModel>()),
				Arguments.of(listOf(event(1, OUT, SHOPPING), event(3, IN)), listOf(closed(1, 3))),
				Arguments.of(listOf(event(1, OUT, SHOPPING)), listOf(ongoing(1))),
				Arguments.of(
					listOf(event(1, OUT, SHOPPING), event(3, IN), event(5, OUT, SHOPPING)),
					listOf(closed(1, 3), ongoing(5)),
				),
				Arguments.of(listOf(event(1, OUT, SHOPPING), event(2, OUT, SHOPPING), event(4, IN)), listOf(closed(1, 4))),
				Arguments.of(listOf(event(1, OUT, SHOPPING), event(6, OUT, DEFINITIVE_DEPARTURE)), listOf(closed(1, 6))),
				Arguments.of(listOf(event(1, OUT, DEFINITIVE_DEPARTURE)), emptyList<OutingModel>()),
				Arguments.of(listOf(event(1, IN), event(2, IN)), emptyList<OutingModel>()),
				Arguments.of(
					listOf(event(1, IN), event(2, OUT, SHOPPING), event(3, IN), event(4, OUT, DEFINITIVE_DEPARTURE)),
					listOf(closed(2, 3)),
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should deduce the outings from the movements`(events: List<MovementEventModel>, expected: List<OutingModel>) {
		// Arrange

		// Act
		val outings = events.toOutings(now)

		// Assert
		assertEquals(expected, outings)
	}

	@Test
	fun `Should count the time out inside the window only`() {
		// Arrange
		val window = TimelineWindowModel(at(10), at(20))
		val outings = listOf(closed(5, 12), closed(15, 18), closed(19, 30))

		// Act
		val seconds = outings.outSeconds(window)

		// Assert
		assertEquals((2 + 3 + 1) * 3600L, seconds)
	}

	@Test
	fun `Should count the whole time out without a window`() {
		// Arrange
		val outings = listOf(closed(1, 3), ongoing(98))

		// Act
		val seconds = outings.outSeconds(null)

		// Assert
		assertEquals((2 + 2) * 3600L, seconds)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should compute the usage rate`(window: TimelineWindowModel?, outings: List<OutingModel>, expected: Double?) {
		// Arrange

		// Act
		val rate = outings.usageRate(window)

		// Assert
		assertEquals(expected, rate)
	}
}
