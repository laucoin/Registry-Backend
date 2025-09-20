package fr.laucoin.registry.backend.domain.validator

import fr.laucoin.registry.backend.domain.annotation.MovementReason
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.VISIT
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.GuestMovementWriterDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto
import fr.laucoin.registry.backend.test.ModelExt.activityId
import jakarta.validation.ConstraintValidatorContext
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock

class MovementReasonValidatorTest {
	private val movementReasonValidator = MovementReasonValidator()

	private companion object {
		@JvmStatic
		fun `Should isValid evaluate if movement reason is valid`(): Stream<Arguments> = Stream.of(
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.EMERGENCY,
					content = null,
					guests = null,
				),
				true
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.LOGISTICS,
					content = null,
					guests = null,
				),
				true
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.PARTNER_ANIMATION,
					content = null,
					guests = null,
				),
				true
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = VISIT,
					content = null,
					guests = null,
				),
				true
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.SHOPPING,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.MEDICAL,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.DEFINITIVE_DEPARTURE,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.OTHER,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.EMERGENCY,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.LOGISTICS,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.PARTNER_ANIMATION,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = VISIT,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.SHOPPING,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.MEDICAL,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.DEFINITIVE_DEPARTURE,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.OTHER,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.EMERGENCY,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.LOGISTICS,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.PARTNER_ANIMATION,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = VISIT,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.SHOPPING,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.MEDICAL,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.DEFINITIVE_DEPARTURE,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = MovementReasonEnum.OTHER,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.EMERGENCY,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.LOGISTICS,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.PARTNER_ANIMATION,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = VISIT,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.SHOPPING,
					activityId = null,
					content = null,
				),
				true
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.MEDICAL,
					activityId = null,
					content = null,
				),
				true
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.DEFINITIVE_DEPARTURE,
					activityId = null,
					content = null,
				),
				true
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = MovementReasonEnum.OTHER,
					activityId = null,
					content = null,
				),
				true
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					activityId = activityId,
					content = null,
				),
				true
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					activityId = null,
					content = null,
				),
				false
			),
			Arguments.of(
				REGISTERED,
				true,
				ParticipantMovementWriterDto(
					dateTime = null,
					type = IN,
					reason = null,
					activityId = null,
					content = null,
				),
				true
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = null,
					content = null,
					guests = null,
				),
				true
			),
			Arguments.of(
				GUEST,
				false,
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
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = OUT,
					reason = VISIT,
					content = null,
					guests = null,
				),
				false
			),
			Arguments.of(
				GUEST,
				false,
				GuestMovementWriterDto(
					dateTime = null,
					type = null,
					reason = null,
					content = null,
					guests = null,
				),
				true
			),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isValid evaluate if movement reason is valid`(
		participantType: ParticipantTypeEnum,
		hasActivity: Boolean,
		movement: Any,
		expected: Boolean,
	) {
		// Arrange
		movementReasonValidator.initialize(
			MovementReason(
				participantType = participantType,
				hasActivity = hasActivity,
				message = "TEST_MESSAGE"
			)
		)
		val context: ConstraintValidatorContext = mock()

		// Act
		val result = movementReasonValidator.isValid(movement, context)

		// Assert
		assertEquals(expected, result)
	}
}
