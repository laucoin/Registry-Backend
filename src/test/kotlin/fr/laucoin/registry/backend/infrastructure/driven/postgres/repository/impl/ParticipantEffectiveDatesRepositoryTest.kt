package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ParticipantEffectiveDatesRepositoryTest : TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var movementPort: IMovementPort

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private val base: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
	private val guestEntry: ZonedDateTime = base.minusHours(5).minusMinutes(30)
	private val guestExit: ZonedDateTime = base.minusHours(2).minusMinutes(30)
	private lateinit var registeredId: UUID
	private lateinit var guestId: UUID
	private lateinit var firstEntryMovementId: UUID

	@Test
	@Order(1)
	fun `Should refresh set the first entry as arrival and no departure`() {
		// Arrange
		registeredId = createParticipant("registered", REGISTERED)
		guestId = createParticipant("guest", GUEST)
		firstEntryMovementId = createMovement(registeredId, IN, null, base.minusHours(5))
		createMovement(registeredId, IN, null, base.minusHours(4))
		createMovement(registeredId, OUT, SHOPPING, base.minusHours(3))
		createMovement(guestId, IN, null, guestEntry)

		// Act
		participantPort.refreshEffectiveDates(listOf(registeredId, guestId)).block()

		// Assert
		assertEquals(base.minusHours(5).toInstant(), arrivalOf(registeredId)?.toInstant())
		assertNull(departureOf(registeredId))
		assertEquals(guestEntry.toInstant(), arrivalOf(guestId)?.toInstant())
		assertNull(departureOf(guestId))
	}

	@Test
	@Order(2)
	fun `Should refresh set the departure on a definitive departure and on a guest exit`() {
		// Arrange
		createMovement(registeredId, OUT, DEFINITIVE_DEPARTURE, base.minusHours(2))
		createMovement(guestId, OUT, null, guestExit)

		// Act
		participantPort.refreshEffectiveDates(listOf(registeredId, guestId)).block()

		// Assert
		assertEquals(base.minusHours(2).toInstant(), departureOf(registeredId)?.toInstant())
		assertEquals(guestExit.toInstant(), departureOf(guestId)?.toInstant())
	}

	@Test
	@Order(3)
	fun `Should refresh clear the departure when the participant enters again`() {
		// Arrange
		createMovement(registeredId, IN, null, base.minusHours(1))

		// Act
		participantPort.refreshEffectiveDates(listOf(registeredId)).block()

		// Assert
		assertNull(departureOf(registeredId))
		assertEquals(base.minusHours(5).toInstant(), arrivalOf(registeredId)?.toInstant())
	}

	@Test
	@Order(4)
	fun `Should refresh ignore a hidden movement when computing the arrival`() {
		// Arrange
		databaseClient.sql("UPDATE tb_movement SET visible = FALSE WHERE id = :id")
			.bind("id", firstEntryMovementId).fetch().rowsUpdated().block()

		// Act
		participantPort.refreshEffectiveDates(listOf(registeredId)).block()

		// Assert
		assertEquals(base.minusHours(4).toInstant(), arrivalOf(registeredId)?.toInstant())
	}

	@Test
	@Order(5)
	fun `Should refresh accept an empty list`() {
		// Act
		val result = participantPort.refreshEffectiveDates(emptyList()).block()

		// Assert
		assertEquals(Unit, result)
	}

	private fun arrivalOf(id: UUID): ZonedDateTime? =
		participantPort.findById(defaultProjectId, id, null).block()!!.effectiveArrival

	private fun departureOf(id: UUID): ZonedDateTime? =
		participantPort.findById(defaultProjectId, id, null).block()!!.effectiveDeparture

	private fun createParticipant(lastName: String, participantType: ParticipantTypeEnum): UUID {
		val participant = ParticipantModel().apply {
			firstName = "effective"
			this.lastName = lastName
			birthday = LocalDate.EPOCH
			type = participantType
			this.projectId = defaultProjectId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		return participantPort.create(participant).block()!!.id!!
	}

	private fun createMovement(
		participantId: UUID,
		movementType: MovementTypeEnum,
		reason: MovementReasonEnum?,
		at: ZonedDateTime,
	): UUID {
		val movement = MovementModel().apply {
			dateTime = at
			type = movementType
			this.reason = reason
			contentType = if (participantId == guestId) GUEST else REGISTERED
			content =
				listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = participantId } })
			this.projectId = defaultProjectId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		return movementPort.create(movement).block()!!.id!!
	}
}
