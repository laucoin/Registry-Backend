package fr.laucoin.registry.backend.domain.enumeration

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.Companion.isMissingActivity
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.Companion.isMissingActivityCommunication
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
        fun `Should isMissingActivity evaluate if missing ACTIVITY`(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(ACTIVITY), false),
            Arguments.of(listOf(PHONE_COMMUNICATION), false),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION), true),
            Arguments.of(listOf(SMOKE_REPORT), false),
            Arguments.of(listOf(MOVEMENT_REPORT), false),
            Arguments.of(listOf(TICKETING), false),
            Arguments.of(listOf(VEHICLE), false),
            Arguments.of(listOf(FIRE_RISK), false),
        )

        @JvmStatic
        fun `Should isMissingActivityCommunication evaluate if missing ACTIVITY_COMMUNICATION`(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(ACTIVITY), false),
            Arguments.of(listOf(PHONE_COMMUNICATION), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, SMOKE_REPORT), false),
            Arguments.of(listOf(SMOKE_REPORT), true),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT, SMOKE_REPORT), false),
            Arguments.of(listOf(MOVEMENT_REPORT), true),
            Arguments.of(listOf(TICKETING), false),
            Arguments.of(listOf(VEHICLE), false),
            Arguments.of(listOf(FIRE_RISK), false),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should isMissingActivity evaluate if missing ACTIVITY`(
        eventOptions: List<EventOptionEnum>,
        expected: Boolean,
    ) {
        // Arrange
        // Act
        val result = eventOptions.isMissingActivity()

        // Assert
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should isMissingActivityCommunication evaluate if missing ACTIVITY_COMMUNICATION`(
        eventOptions: List<EventOptionEnum>,
        expected: Boolean,
    ) {
        // Arrange
        // Act
        val result = eventOptions.isMissingActivityCommunication()

        // Assert
        assertEquals(expected, result)
    }
}
