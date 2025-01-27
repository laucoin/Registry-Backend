package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.FIRE_RISK
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.PHONE_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.TICKETING
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus.BAD_REQUEST

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
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION, SMOKE_REPORT), true),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION, MOVEMENT_REPORT), true),
            Arguments.of(listOf(ACTIVITY, ACTIVITY_COMMUNICATION, MOVEMENT_REPORT, SMOKE_REPORT), true),
            Arguments.of(listOf(TICKETING), true),
            Arguments.of(listOf(VEHICLE), true),
            Arguments.of(listOf(FIRE_RISK), true),
        )

        @JvmStatic
        fun `Should isValid throw if Event options are invalid`(): Stream<Arguments> = Stream.of(
            Arguments.of(listOf(ACTIVITY_COMMUNICATION), arrayListOf(ACTIVITY_COMMUNICATION, ACTIVITY)),
            Arguments.of(listOf(SMOKE_REPORT), arrayListOf(SMOKE_REPORT, ACTIVITY_COMMUNICATION)),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, SMOKE_REPORT), arrayListOf(ACTIVITY_COMMUNICATION, ACTIVITY)),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT), arrayListOf(ACTIVITY_COMMUNICATION, ACTIVITY)),
            Arguments.of(listOf(ACTIVITY_COMMUNICATION, MOVEMENT_REPORT, SMOKE_REPORT), arrayListOf(ACTIVITY_COMMUNICATION, ACTIVITY)),
            Arguments.of(listOf(MOVEMENT_REPORT), arrayListOf(MOVEMENT_REPORT, ACTIVITY_COMMUNICATION)),
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

    @ParameterizedTest
    @MethodSource
    fun `Should isValid throw if Event options are invalid`(
        eventOptions: List<EventOptionEnum>?,
        expected: ArrayList<EventOptionEnum>,
    ) {
        // Arrange
        val context: ConstraintValidatorContext = mock()

        // Act
        val result = assertThrows(RegistryException::class.java) {
            optionValidator.isValid(eventOptions, context)
        }

        // Assert
        assertEquals(BAD_REQUEST, result.status)
        assertEquals(EVENT_OPTIONS_MISSING, result.code)
        assertEquals(expected.size, result.args?.size)
        assertTrue(expected.map(EventOptionEnum::name).containsAll(result.args !!))
    }
}
