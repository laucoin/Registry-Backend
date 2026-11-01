package fr.laucoin.registry.backend.domain.extension

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
}
