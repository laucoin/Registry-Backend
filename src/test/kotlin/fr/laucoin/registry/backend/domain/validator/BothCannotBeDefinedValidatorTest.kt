package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.BothCannotBeDefined
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import org.apache.logging.log4j.util.Strings.EMPTY
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class BothCannotBeDefinedValidatorTest {
	private val bothCannotBeDefinedValidator = BothCannotBeDefinedValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate if at least one field is not null`(): Stream<Arguments> = Stream.of(
			Arguments.of(ClassWithFields(), true),
			Arguments.of(ClassWithFields(element1 = EMPTY, element2 = null), true),
			Arguments.of(ClassWithFields(element1 = null, element2 = EMPTY), true),
			Arguments.of(ClassWithFields(element1 = EMPTY, element2 = EMPTY), false),
		)

		@JvmStatic
		fun `Should isValid evaluate if at least both are not empty`(): Stream<Arguments> = Stream.of(
			Arguments.of(ClassWithFields(), true),
			Arguments.of(ClassWithFields(list1 = emptyList(), list2 = null), true),
			Arguments.of(ClassWithFields(list1 = null, list2 = emptyList()), true),
			Arguments.of(ClassWithFields(list1 = emptyList(), list2 = emptyList()), true),
			Arguments.of(ClassWithFields(list1 = listOf(EMPTY), list2 = null), true),
			Arguments.of(ClassWithFields(list1 = null, list2 = listOf(EMPTY)), true),
			Arguments.of(ClassWithFields(list1 = listOf(EMPTY), list2 = emptyList()), true),
			Arguments.of(ClassWithFields(list1 = emptyList(), list2 = listOf(EMPTY)), true),
			Arguments.of(ClassWithFields(list1 = listOf(EMPTY), list2 = listOf(EMPTY)), false),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if at least one field is not null`(
		value: ClassWithFields,
		expected: Boolean,
	) {
		// Arrange
		bothCannotBeDefinedValidator.initialize(
			BothCannotBeDefined(
				first = "element1",
				second = "element2",
				message = "TEST_MESSAGE"
			)
		)
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = bothCannotBeDefinedValidator.isValid(value, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if at least both are not empty`(
		value: ClassWithFields,
		expected: Boolean,
	) {
		// Arrange
		bothCannotBeDefinedValidator.initialize(
			BothCannotBeDefined(
				first = "list1",
				second = "list2",
				message = "TEST_MESSAGE"
			)
		)
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = bothCannotBeDefinedValidator.isValid(value, context)

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
