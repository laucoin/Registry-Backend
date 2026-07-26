package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MinUpperMax
import fr.laucoin.registry.backend.domain.constant.ErrorConst.COMPARING_WRONG_PARAMETER_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME
import fr.laucoin.registry.backend.domain.model.RegistryException
import jakarta.validation.ConstraintValidatorContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import java.util.stream.Stream

class MinUpperMaxValidatorTest {
	private val numberValidator = MinUpperMaxValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid throw for no-existing specified field`(): Stream<Arguments> = Stream.of(
			Arguments.of("test", "end"),
			Arguments.of("start", "test"),
			Arguments.of("test", "test"),
		)

		@JvmStatic
		fun `Should isValid validate Int values`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, 10, true),
			Arguments.of(1, null, true),
			Arguments.of(10, 1, false),
			Arguments.of(1, 10, true),
		)

		@JvmStatic
		fun `Should isValid validate Double values`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, 10.0, true),
			Arguments.of(1.0, null, true),
			Arguments.of(10.0, 1.0, false),
			Arguments.of(1.0, 10.0, true),
		)

		@JvmStatic
		fun `Should isValid validate Float values`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, 10F, true),
			Arguments.of(1F, null, true),
			Arguments.of(10F, 1F, false),
			Arguments.of(1F, 10F, true),
		)

		@JvmStatic
		fun `Should isValid validate Long values`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, true),
			Arguments.of(null, 10L, true),
			Arguments.of(1L, null, true),
			Arguments.of(10L, 1L, false),
			Arguments.of(1L, 10L, true),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid throw for no-existing specified field`(
		startField: String,
		endField: String,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithNumericValue(minInt = 1, maxInt = 10)
		numberValidator.initialize(MinUpperMax(startField = startField, endField = endField, message = "TEST_MESSAGE"))

		// Act
		val result = assertThrows(RegistryException::class.java) {
			numberValidator.isValid(data, context)
		}

		// Assert
		assertEquals(INTERNAL_SERVER_ERROR, result.status)
		assertEquals(NO_PARAMETER_FOUND_FOR_SPECIFIED_NAME, result.message)
	}

	@Test
	fun `Should isValid throw for different field type`() {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithNumericValue(minInt = 1, maxDouble = 10.0)
		numberValidator.initialize(
			MinUpperMax(
				startField = "minInt",
				endField = "maxDouble",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = assertThrows(RegistryException::class.java) {
			numberValidator.isValid(data, context)
		}

		// Assert
		assertEquals(INTERNAL_SERVER_ERROR, result.status)
		assertEquals(COMPARING_WRONG_PARAMETER_TYPE, result.message)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate Int values`(
		start: Int?,
		end: Int?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithNumericValue(minInt = start, maxInt = end)
		numberValidator.initialize(MinUpperMax(startField = "minInt", endField = "maxInt", message = "TEST_MESSAGE"))

		// Act
		val result = numberValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate Double values`(
		start: Double?,
		end: Double?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithNumericValue(minDouble = start, maxDouble = end)
		numberValidator.initialize(
			MinUpperMax(
				startField = "minDouble",
				endField = "maxDouble",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = numberValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate Float values`(
		start: Float?,
		end: Float?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithNumericValue(minFloat = start, maxFloat = end)
		numberValidator.initialize(
			MinUpperMax(
				startField = "minFloat",
				endField = "maxFloat",
				message = "TEST_MESSAGE"
			)
		)

		// Act
		val result = numberValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid validate Long values`(
		start: Long?,
		end: Long?,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()
		val data = ClassWithNumericValue(minLong = start, maxLong = end)
		numberValidator.initialize(MinUpperMax(startField = "minLong", endField = "maxLong", message = "TEST_MESSAGE"))

		// Act
		val result = numberValidator.isValid(data, context)

		// Assert
		assertEquals(expected, result)
	}

	data class ClassWithNumericValue(
		val minInt: Int? = null,
		val maxInt: Int? = null,
		val minDouble: Double? = null,
		val maxDouble: Double? = null,
		val minFloat: Float? = null,
		val maxFloat: Float? = null,
		val minLong: Long? = null,
		val maxLong: Long? = null,
	)
}
