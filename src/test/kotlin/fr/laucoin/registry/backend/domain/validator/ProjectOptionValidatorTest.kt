package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.FIRE_RISK
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.MOVEMENT_REPORT
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.PHONE_COMMUNICATION
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.SMOKE_REPORT
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.TICKETING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.VEHICLE
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

class ProjectOptionValidatorTest {
    private val optionValidator = ProjectOptionValidator()

    companion object {
        @JvmStatic
        fun `Should isValid evaluate if Project options are valid`(): Stream<Arguments> = Stream.of(
            Arguments.of(null, true),
            Arguments.of(emptyList<ProjectOptionEnum>(), true),
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
        fun `Should isValid throw if Project options are invalid`(): Stream<Arguments> = Stream.of(
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
    fun `Should isValid evaluate if Project options are valid`(
        projectOptions: List<ProjectOptionEnum>?,
        expected: Boolean,
    ) {
        // Arrange
        val context: ConstraintValidatorContext = mock()

        // Act
        val result = optionValidator.isValid(projectOptions, context)

        // Assert
        assertEquals(expected, result)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should isValid throw if Project options are invalid`(
        projectOptions: List<ProjectOptionEnum>?,
        expected: ArrayList<ProjectOptionEnum>,
    ) {
        // Arrange
        val context: ConstraintValidatorContext = mock()

        // Act
        val result = assertThrows(RegistryException::class.java) {
            optionValidator.isValid(projectOptions, context)
        }

        // Assert
        assertEquals(BAD_REQUEST, result.status)
        assertEquals(PROJECT_OPTIONS_MISSING, result.code)
        assertEquals(expected.size, result.args?.size)
        assertTrue(expected.map(ProjectOptionEnum::name).containsAll(result.args !!))
    }
}
