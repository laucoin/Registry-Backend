package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PlannedMovementModelTest {
	@Test
	fun `Should have no reason for an arrival and the definitive departure reason for a departure`() {
		// Arrange
		val participants = listOf(ParticipantModel())

		// Act
		val arrival = PlannedMovementModel(IN, null, participants)
		val departure = PlannedMovementModel(OUT, null, participants)

		// Assert
		assertNull(arrival.reason)
		assertEquals(DEFINITIVE_DEPARTURE, departure.reason)
	}

	@Test
	fun `Should refuse a planned movement without participant`() {
		// Arrange
		val participants = emptyList<ParticipantModel>()

		// Act
		val build = { PlannedMovementModel(IN, null, participants, "Alpha") }

		// Assert
		assertThrows<IllegalArgumentException> { build() }
	}

	@Test
	fun `Should refuse several participants without a shared pool name`() {
		// Arrange
		val participants = listOf(ParticipantModel(), ParticipantModel())

		// Act
		val build = { PlannedMovementModel(IN, null, participants) }

		// Assert
		assertThrows<IllegalArgumentException> { build() }
	}
}
