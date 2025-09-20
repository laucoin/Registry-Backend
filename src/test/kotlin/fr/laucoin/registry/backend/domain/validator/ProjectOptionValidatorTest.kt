package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ProjectError.PROJECT_OPTIONS_MISSING
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.COMMUNICATION
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
			Arguments.of(listOf(ACTIVITY, COMMUNICATION), true),
			Arguments.of(listOf(VEHICLE), true),
		)

		@JvmStatic
		fun `Should isValid throw if Project options are invalid`(): Stream<Arguments> = Stream.of(
			Arguments.of(listOf(COMMUNICATION), arrayListOf(ACTIVITY)),
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
		assertTrue(expected.map(ProjectOptionEnum::name).containsAll(result.args!!))
	}
}
