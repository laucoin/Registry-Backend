package fr.laucoin.registry.backend.infrastructure.driving.api.validation.validator

import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class ValidEmailsValidatorTest {
	private val validator = ValidEmailsValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate every email of the list`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, true),
			Arguments.of(emptyList<String>(), true),
			Arguments.of(listOf("john@doe.fr"), true),
			Arguments.of(listOf("john@doe.fr", "jane.doe+tag@sgdf.fr"), true),
			Arguments.of(listOf("john@doe.fr", "not-an-email"), false),
			Arguments.of(listOf("john@doe"), false),
			Arguments.of(listOf("john doe@doe.fr"), false),
			Arguments.of(listOf(""), false),
			Arguments.of(listOf(null), false),
			Arguments.of(listOf("${"a".repeat(320)}@sgdf.fr"), false),
			Arguments.of(listOf("${"a".repeat(312)}@sgdf.fr"), true),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate every email of the list`(emails: List<String?>?, expected: Boolean) {
		// Arrange
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = validator.isValid(emails, context)

		// Assert
		assertEquals(expected, result)
	}
}
