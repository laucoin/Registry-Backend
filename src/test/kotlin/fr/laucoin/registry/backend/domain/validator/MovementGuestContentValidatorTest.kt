package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto.GuestWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.MovementContentWriterDto
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class MovementGuestContentValidatorTest {
	private val movementContentValidator = MovementGuestContentValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate if specified content is consistent`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					content = null,
					guests = listOf(GuestWriterDto()),
				),
				true
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					content = emptyList(),
					guests = listOf(GuestWriterDto()),
				),
				true
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					content = listOf(MovementContentWriterDto()),
					guests = listOf(GuestWriterDto()),
				),
				false
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					content = null,
					guests = emptyList(),
				),
				false
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					content = listOf(MovementContentWriterDto()),
					guests = null,
				),
				true
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					content = listOf(MovementContentWriterDto()),
					guests = emptyList(),
				),
				true
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					content = listOf(MovementContentWriterDto()),
					guests = listOf(GuestWriterDto()),
				),
				false
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					content = emptyList(),
					guests = null,
				),
				false
			),
			Arguments.of(
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					content = null,
					guests = null,
				),
				false
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if specified content is consistent`(
		movement: GuestMovementWriterDto,
		expected: Boolean,
	) {
		// Arrange
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = movementContentValidator.isValid(movement, context)

		// Assert
		assertEquals(expected, result)
	}
}
