package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.Companion.missingOptions
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.FIRE_RISK
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.PHONE_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.TICKETING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.VEHICLE
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class EventOptionEnumTest {
    companion object {
        @JvmStatic
        fun `Should missingOptions evaluate if missing any option`(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(ACTIVITY), null),
            Arguments.of(listOf(PHONE_COMMUNICATION), null),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION), null),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION), Pair(ACTIVITY_COMMUNICATION, listOf(ACTIVITY))),
            Arguments.of(listOf(SMOKE_REPORT), Pair(SMOKE_REPORT, listOf(ACTIVITY_COMMUNICATION))),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, SMOKE_REPORT), Pair(ACTIVITY_COMMUNICATION, listOf(ACTIVITY))),
            Arguments.of(listOf(MOVEMENT_REPORT), Pair(MOVEMENT_REPORT, listOf(ACTIVITY_COMMUNICATION))),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT), Pair(ACTIVITY_COMMUNICATION, listOf(ACTIVITY))),
            Arguments.of(listOf(TICKETING), null),
            Arguments.of(listOf(VEHICLE), null),
            Arguments.of(listOf(FIRE_RISK), null),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should missingOptions evaluate if missing any option`(
        eventOptions: List<EventOptionEnum>,
        expected: Pair<EventOptionEnum, List<EventOptionEnum>>?,
    ) {
        // Arrange
        // Act
        val result = eventOptions.missingOptions()

        // Assert
        assertEquals(expected, result)
    }
}
