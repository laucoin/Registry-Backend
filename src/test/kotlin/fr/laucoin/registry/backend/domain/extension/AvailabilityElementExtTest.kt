package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.DEPARTED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildAvailabilityWarning
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.stream.Stream
import kotlin.test.assertEquals

class AvailabilityElementExtTest {
	companion object {
		@JvmStatic
		fun `Should build a presence status from departure, movement and window`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(false, null, true, OUT, false),
				Arguments.of(false, null, false, UNAVAILABLE, false),
				Arguments.of(false, MovementTypeEnum.IN, true, IN, false),
				Arguments.of(false, MovementTypeEnum.IN, false, IN, true),
				Arguments.of(false, MovementTypeEnum.OUT, true, OUT, false),
				Arguments.of(false, MovementTypeEnum.OUT, false, OUT, true),
				Arguments.of(true, MovementTypeEnum.OUT, true, DEPARTED, false),
				Arguments.of(true, MovementTypeEnum.OUT, false, DEPARTED, false),
			)
	}

	private fun participant(departed: Boolean, available: Boolean): ParticipantModel =
		ParticipantModel().apply {
			departedAt = if (departed) ZonedDateTime.now() else null
			endAvailability = if (available) null else CustomDateTimeModel(LocalDate.now().minusDays(5), null)
		}

	@ParameterizedTest
	@MethodSource
	fun `Should build a presence status from departure, movement and window`(
		departed: Boolean,
		lastMovementType: MovementTypeEnum?,
		available: Boolean,
		expectedStatus: PresenceStatusEnum,
		expectedWarning: Boolean,
	) {
		// Arrange
		val participant = participant(departed, available)

		// Act
		val status = participant.buildStatus(lastMovementType)
		val warning = participant.buildAvailabilityWarning(lastMovementType)

		// Assert
		assertEquals(expectedStatus, status)
		assertEquals(expectedWarning, warning)
	}
}
