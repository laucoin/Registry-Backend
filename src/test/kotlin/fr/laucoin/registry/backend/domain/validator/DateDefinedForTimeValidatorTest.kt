package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.CustomDateTimeWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.time.LocalDate
import java.time.OffsetTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class DateDefinedForTimeValidatorTest {
	private val dateDefinedForTimeValidator = DateDefinedForTimeValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate if time is not the only defined`(): Stream<Arguments> = Stream.of(
			Arguments.of(CustomDateTimeWriterDto(), true),
			Arguments.of(CustomDateTimeWriterDto(date = LocalDate.EPOCH), true),
			Arguments.of(CustomDateTimeWriterDto(date = LocalDate.EPOCH, time = OffsetTime.now()), true),
			Arguments.of(CustomDateTimeWriterDto(time = OffsetTime.now()), false),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if time is not the only defined`(
		value: CustomDateTimeWriterDto,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = dateDefinedForTimeValidator.isValid(value, context)

		// Assert
		assertEquals(expected, result)
	}
}
