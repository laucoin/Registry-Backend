package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN as PRESENT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT as ABSENT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.UNAVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ParticipantPresenceStatusRepositoryTest: TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var movementPort: IMovementPort

	private val base: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS)
	private val pageable = PageableModel(0, 200)
	private lateinit var expectedNotArrivedId: UUID
	private lateinit var notArrivedOutOfRangeId: UUID
	private lateinit var earlyArrivalId: UUID
	private lateinit var outingId: UUID
	private lateinit var departedId: UUID

	@Test
	@Order(1)
	fun `Should create participants in every presence situation`() {
		// Arrange
		expectedNotArrivedId = createParticipant("expected", start = -1, end = 1)
		notArrivedOutOfRangeId = createParticipant("ended", start = -5, end = -2)
		earlyArrivalId = createParticipant("early", start = 3, end = 6)
		outingId = createParticipant("outing", start = -1, end = 1)
		departedId = createParticipant("departed", start = -1, end = 1)

		// Act
		createMovement(earlyArrivalId, IN, null, base.minusMinutes(50))
		createMovement(outingId, IN, null, base.minusMinutes(40))
		createMovement(outingId, OUT, SHOPPING, base.minusMinutes(30))
		createMovement(departedId, IN, null, base.minusMinutes(20))
		createMovement(departedId, OUT, DEFINITIVE_DEPARTURE, base.minusMinutes(10))
		participantPort.refreshEffectiveDates(listOf(earlyArrivalId, outingId, departedId)).block()

		// Assert
		assertEquals(PRESENT, statusOf(earlyArrivalId))
	}

	@Test
	@Order(2)
	fun `Should derive the presence status from the effective dates and the last movement`() {
		// Assert
		assertEquals(ABSENT, statusOf(expectedNotArrivedId))
		assertEquals(UNAVAILABLE, statusOf(notArrivedOutOfRangeId))
		assertEquals(PRESENT, statusOf(earlyArrivalId))
		assertEquals(ABSENT, statusOf(outingId))
		assertEquals(UNAVAILABLE, statusOf(departedId))
	}

	@Test
	@Order(3)
	fun `Should findPage filter on each presence status`() {
		// Act
		val present = idsWithStatus(PRESENT)
		val absent = idsWithStatus(ABSENT)
		val unavailable = idsWithStatus(UNAVAILABLE)

		// Assert
		assertTrue(present.contains(earlyArrivalId))
		assertTrue(absent.containsAll(listOf(expectedNotArrivedId, outingId)))
		assertTrue(unavailable.containsAll(listOf(notArrivedOutOfRangeId, departedId)))
		assertTrue(present.none { absent.contains(it) || unavailable.contains(it) })
	}

	private fun statusOf(id: UUID): PresenceStatusEnum? = participantPort.findById(projectId, id, null).block()!!.status

	private fun idsWithStatus(status: PresenceStatusEnum): List<UUID> {
		return participantPort.findPage(projectId, pageable, ParticipantSearchParamModel(query = null, status = status, at = null))
			.block()!!.content.mapNotNull { it.id }
	}

	private fun createParticipant(lastName: String, start: Long, end: Long): UUID {
		val participant = ParticipantModel().apply {
			firstName = "status"
			this.lastName = lastName
			birthday = LocalDate.EPOCH
			type = REGISTERED
			startAvailability = CustomDateTimeModel(LocalDate.now().plusDays(start))
			endAvailability = CustomDateTimeModel(LocalDate.now().plusDays(end))
			this.projectId = fr.laucoin.registry.backend.test.ModelExt.projectId
			create(currentUser())
		}
		return participantPort.create(participant).block()!!.id!!
	}

	private fun createMovement(
		participantId: UUID,
		movementType: MovementTypeEnum,
		reason: MovementReasonEnum?,
		at: ZonedDateTime,
	) {
		val movement = MovementModel(contentType = REGISTERED).apply {
			dateTime = at
			type = movementType
			this.reason = reason
			content = listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = participantId } })
			this.projectId = fr.laucoin.registry.backend.test.ModelExt.projectId
			create(currentUser())
		}
		movementPort.create(movement).block()
	}
}
