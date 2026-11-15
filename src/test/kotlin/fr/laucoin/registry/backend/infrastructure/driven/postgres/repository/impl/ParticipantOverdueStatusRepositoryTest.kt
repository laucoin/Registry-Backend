package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ParticipantSearchParamModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
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
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ParticipantOverdueStatusRepositoryTest : TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var movementPort: IMovementPort

	private val base: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
	private val pageable = PageableModel(0, 200)
	private var minutes = 0L
	private val ids = mutableMapOf<String, UUID>()

	@Test
	@Order(1)
	fun `Should create participants in every planned end situation`() {
		// Arrange
		val ended = createGroup("ended", -10, -1)
		val future = createGroup("future", 5, 9)
		val current = createGroup("current", -2, 2)
		val open = createGroup(null, null)

		// Act
		arrive("ownPast", ownEnd = -1)
		arrive("ownFuture", ownEnd = 1)
		arrive("groupEnded", groups = listOf(ended))
		arrive("groupGap", groups = listOf(ended, future))
		arrive("groupOverlap", groups = listOf(ended, current))
		arrive("groupPermanent", groups = listOf(ended, open))
		arrive("noGroup")
		arrive("ownEndWins", ownEnd = 1, groups = listOf(ended))
		arrive("outOverdue", ownEnd = -1, thenOut = true)
		arrive("departed", ownEnd = -1, thenDeparture = true)
		ids["neverArrived"] = createParticipant("neverArrived", ownEnd = -3, groups = emptyList())
		participantPort.refreshEffectiveDates(ids.values.toList()).block()

		// Assert
		assertEquals(11, ids.size)
	}

	@Test
	@Order(2)
	fun `Should flag as overdue only the arrived participants whose planned end has passed`() {
		// Arrange
		val names = ids.keys

		// Act
		val statuses = names.associateWith { statusOf(it) }

		// Assert
		assertEquals(PresenceStatusEnum.OVERDUE, statuses.getValue("ownPast"))
		assertEquals(PresenceStatusEnum.OVERDUE, statuses.getValue("groupEnded"))
		assertEquals(PresenceStatusEnum.OVERDUE, statuses.getValue("groupGap"))
		assertEquals(PresenceStatusEnum.OVERDUE, statuses.getValue("outOverdue"))
		assertEquals(PresenceStatusEnum.IN, statuses.getValue("ownFuture"))
		assertEquals(PresenceStatusEnum.IN, statuses.getValue("groupOverlap"))
		assertEquals(PresenceStatusEnum.IN, statuses.getValue("groupPermanent"))
		assertEquals(PresenceStatusEnum.IN, statuses.getValue("noGroup"))
		assertEquals(PresenceStatusEnum.IN, statuses.getValue("ownEndWins"))
		assertEquals(PresenceStatusEnum.UNAVAILABLE, statuses.getValue("departed"))
		assertEquals(PresenceStatusEnum.UNAVAILABLE, statuses.getValue("neverArrived"))
	}

	@Test
	@Order(3)
	fun `Should findPage filter on the overdue status`() {
		// Arrange
		val params = ParticipantSearchParamModel(query = null, status = PresenceStatusEnum.OVERDUE, at = null)

		// Act
		val overdue = idsWith(params)

		// Assert
		assertTrue(overdue.containsAll(listOf("ownPast", "groupEnded", "groupGap", "outOverdue").map { ids[it] }))
		assertTrue(overdue.none {
			listOf(
				"ownFuture",
				"groupOverlap",
				"groupPermanent",
				"noGroup",
				"ownEndWins",
				"departed",
				"neverArrived"
			).map { n -> ids[n] }.contains(it)
		})
	}

	@Test
	@Order(4)
	fun `Should findPage count the overdue participants as physically present or absent`() {
		// Arrange
		val inParams = ParticipantSearchParamModel(query = null, presence = PresenceStatusEnum.IN, at = null)
		val outParams = ParticipantSearchParamModel(query = null, presence = PresenceStatusEnum.OUT, at = null)

		// Act
		val present = idsWith(inParams)
		val absent = idsWith(outParams)

		// Assert
		assertTrue(present.containsAll(listOf("ownPast", "groupEnded", "groupGap").map { ids[it] }))
		assertTrue(absent.contains(ids["outOverdue"]))
		assertTrue(present.none { absent.contains(it) })
	}

	private fun statusOf(name: String): PresenceStatusEnum? =
		participantPort.findById(defaultProjectId, ids[name]!!, null).block()!!.status

	private fun idsWith(params: ParticipantSearchParamModel): List<UUID> {
		return participantPort.findPage(defaultProjectId, pageable, params).block()!!.content.mapNotNull { it.id }
	}

	private fun createGroup(start: Long?, end: Long?): UUID = createGroup("group", start, end)

	private fun createGroup(name: String, start: Long?, end: Long?): UUID {
		val group = GroupModel().apply {
			this.name = name
			startAvailability = start?.let { CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).plusDays(it)) }
			endAvailability = end?.let { CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).plusDays(it)) }
			this.projectId = defaultProjectId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		return groupPort.create(group).block()!!.id!!
	}

	private fun arrive(
		name: String,
		ownEnd: Long? = null,
		groups: List<UUID> = emptyList(),
		thenOut: Boolean = false,
		thenDeparture: Boolean = false,
	) {
		val id = createParticipant(name, ownEnd, groups)
		ids[name] = id
		createMovement(id, IN, null)
		if (thenOut) createMovement(id, OUT, SHOPPING)
		if (thenDeparture) createMovement(id, OUT, DEFINITIVE_DEPARTURE)
	}

	private fun createParticipant(name: String, ownEnd: Long?, groups: List<UUID>): UUID {
		val participant = ParticipantModel().apply {
			firstName = "overdue"
			lastName = name
			birthday = LocalDate.EPOCH
			type = REGISTERED
			endAvailability = ownEnd?.let { CustomDateTimeModel(LocalDate.now(ZoneOffset.UTC).plusDays(it)) }
			this.groups = groups.map { GroupModel().apply { id = it } }
			this.projectId = defaultProjectId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		return participantPort.create(participant).block()!!.id!!
	}

	private fun createMovement(participantId: UUID, movementType: MovementTypeEnum, reason: MovementReasonEnum?) {
		minutes += 1
		val movement = MovementModel(contentType = REGISTERED).apply {
			dateTime = base.minusHours(5).plusMinutes(minutes)
			type = movementType
			this.reason = reason
			content =
				listOf(MovementContentModel().apply { participant = ParticipantModel().apply { id = participantId } })
			this.projectId = defaultProjectId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		movementPort.create(movement).block()
	}
}
