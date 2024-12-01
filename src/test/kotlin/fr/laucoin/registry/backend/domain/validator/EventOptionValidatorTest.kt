package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.FIRE_RISK
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.PHONE_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.TICKETING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.VEHICLE
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class EventOptionValidatorTest {
    private val optionValidator = EventOptionValidator()

    companion object {
        @JvmStatic
        fun `Should isValid evaluate if Event options are valid`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, true),
            Arguments.of(emptyList<EventOptionEnum>(), true),
            Arguments.of(listOf(ACTIVITY), true),
            Arguments.of(listOf(PHONE_COMMUNICATION), true),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION), true),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION), false),
            Arguments.of(listOf(SMOKE_REPORT), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, SMOKE_REPORT), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT), false),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT, SMOKE_REPORT), false),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION, SMOKE_REPORT), true),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION, MOVEMENT_REPORT), true),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION, MOVEMENT_REPORT, SMOKE_REPORT), true),
            Arguments.of(listOf(MOVEMENT_REPORT), false),
            Arguments.of(listOf(TICKETING), true),
            Arguments.of(listOf(VEHICLE), true),
            Arguments.of(listOf(FIRE_RISK), true),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should isValid evaluate if Event options are valid`(
        eventOptions: List<EventOptionEnum>?,
        expected: Boolean,
    ) {
        // Arrange
        val context: ConstraintValidatorContext = mock()

        // Act
        val result = optionValidator.isValid(eventOptions, context)

        // Assert
        assertEquals(expected, result)
    }
}
