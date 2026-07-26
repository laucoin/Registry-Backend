package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.AtLeastOneIsDefined
import jakarta.validation.ConstraintValidatorContext
import org.apache.logging.log4j.util.Strings.EMPTY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

class AtLeastOneIsDefinedValidatorTest {
	private val atLeastOneIsDefinedValidator = AtLeastOneIsDefinedValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate if at least one field is not null`(): Stream<Arguments> = Stream.of(
			Arguments.of(ClassWithFields(), false),
			Arguments.of(ClassWithFields(element1 = EMPTY, element2 = null), true),
			Arguments.of(ClassWithFields(element1 = null, element2 = EMPTY), true),
			Arguments.of(ClassWithFields(element1 = EMPTY, element2 = EMPTY), true),
		)

		@JvmStatic
		fun `Should isValid evaluate if at least one list is not empty`(): Stream<Arguments> = Stream.of(
			Arguments.of(ClassWithFields(), false),
			Arguments.of(ClassWithFields(list1 = emptyList(), list2 = null), false),
			Arguments.of(ClassWithFields(list1 = null, list2 = emptyList()), false),
			Arguments.of(ClassWithFields(list1 = emptyList(), list2 = emptyList()), false),
			Arguments.of(ClassWithFields(list1 = listOf(EMPTY), list2 = null), true),
			Arguments.of(ClassWithFields(list1 = null, list2 = listOf(EMPTY)), true),
			Arguments.of(ClassWithFields(list1 = listOf(EMPTY), list2 = emptyList()), true),
			Arguments.of(ClassWithFields(list1 = emptyList(), list2 = listOf(EMPTY)), true),
			Arguments.of(ClassWithFields(list1 = listOf(EMPTY), list2 = listOf(EMPTY)), true),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if at least one field is not null`(
		value: ClassWithFields,
		expected: Boolean,
	) {
		// Arrange
		atLeastOneIsDefinedValidator.initialize(
			AtLeastOneIsDefined(
				first = "element1",
				second = "element2",
				message = "TEST_MESSAGE"
			)
		)
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = atLeastOneIsDefinedValidator.isValid(value, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if at least one list is not empty`(
		value: ClassWithFields,
		expected: Boolean,
	) {
		// Arrange
		atLeastOneIsDefinedValidator.initialize(
			AtLeastOneIsDefined(
				first = "list1",
				second = "list2",
				message = "TEST_MESSAGE"
			)
		)
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = atLeastOneIsDefinedValidator.isValid(value, context)

		// Assert
		assertEquals(expected, result)
	}

	data class ClassWithFields(
		val element1: String? = null,
		val element2: String? = null,
		val list1: List<String>? = null,
		val list2: List<String>? = null,
	)
}
