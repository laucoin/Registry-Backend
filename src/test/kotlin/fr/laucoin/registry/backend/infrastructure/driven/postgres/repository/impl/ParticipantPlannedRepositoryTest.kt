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
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IGroupPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.test.ModelExt
import fr.laucoin.registry.backend.test.TestContext
import fr.laucoin.registry.backend.test.WebTestClientExt.currentUser
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals

@TestInstance(PER_CLASS)
class ParticipantPlannedRepositoryTest : TestContext() {
	@Autowired
	private lateinit var participantPort: IParticipantPort

	@Autowired
	private lateinit var groupPort: IGroupPort

	@Autowired
	private lateinit var movementPort: IMovementPort

	private val tenantId: UUID = ModelExt.defaultProjectId
	private val base: ZonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
	private val day: LocalDate = LocalDate.of(2099, 1, 15)
	private var minutes = 0L
	private val ids = mutableMapOf<String, UUID>()
	private val groupIds = mutableMapOf<String, UUID>()

	@BeforeAll
	fun setUp() {
		createGroups()
		createArrivalParticipants()
		createDepartureParticipants()
		participantPort.refreshEffectiveDates(ids.values.toList()).block()
	}

	@Test
	fun `Should find the arrivals planned on the participant own date`() {
		// Arrange
		val expected = listOf("ownArrival", "ownArrivalAfterTempOut")

		// Act
		val names = ownNames(IN)

		// Assert
		assertEquals(expected, names)
	}

	@Test
	fun `Should return the own planned participants with their planned dates`() {
		// Arrange
		val expected = CustomDateTimeModel(day)

		// Act
		val arrival = participantPort.findPlannedOnOwnDate(tenantId, IN, day, 200).blockFirst()!!
		val departure = participantPort.findPlannedOnOwnDate(tenantId, OUT, day, 200).blockFirst()!!

		// Assert
		assertEquals(expected, arrival.startAvailability)
		assertEquals(expected, departure.endAvailability)
	}

	@Test
	fun `Should find the arrivals inherited from the earliest group date`() {
		// Arrange
		val expected = setOf("groupArrival")

		// Act
		val found = groupNames(IN)

		// Assert
		assertEquals(expected, found.keys)
		assertEquals(groupIds["arrivalGroup"], found["groupArrival"])
	}

	@Test
	fun `Should find the departures planned on the participant own date`() {
		// Arrange
		val expected = listOf("ownDeparture1", "ownDeparture2", "ownDepartureNeverArrived")

		// Act
		val names = ownNames(OUT)

		// Assert
		assertEquals(expected, names)
	}

	@Test
	fun `Should find the departures inherited from the latest group date`() {
		// Arrange
		val expected = setOf("groupDeparture", "groupDepartureOut")

		// Act
		val found = groupNames(OUT)

		// Assert
		assertEquals(expected, found.keys)
		assertEquals(groupIds["departureGroup"], found["groupDeparture"])
	}

	@Test
	fun `Should cap the planned participants of every source`() {
		// Arrange
		val limit = 1

		// Act
		val arrivals = participantPort.findPlannedOnOwnDate(tenantId, IN, day, limit).collectList().block()!!
		val departures = participantPort.findPlannedOnOwnDate(tenantId, OUT, day, limit).collectList().block()!!
		val inherited = participantPort.findPlannedOnGroupDate(tenantId, OUT, day, limit).collectList().block()!!

		// Assert
		assertEquals(1, arrivals.size)
		assertEquals(1, departures.size)
		assertEquals(1, inherited.size)
	}

	@Test
	fun `Should find nothing on a day without planned dates`() {
		// Arrange
		val otherDay = day.plusDays(1000)

		// Act
		val own = participantPort.findPlannedOnOwnDate(tenantId, IN, otherDay, 200).collectList().block()!!
		val inherited = participantPort.findPlannedOnGroupDate(tenantId, OUT, otherDay, 200).collectList().block()!!

		// Assert
		assertEquals(emptyList(), own)
		assertEquals(emptyList(), inherited)
	}

	private fun createGroups() {
		groupIds["arrivalGroup"] = createGroup("arrivalGroup", start = 3, end = 9)
		groupIds["earlyGroup"] = createGroup("earlyGroup", start = 1, end = 3)
		groupIds["lateGroup"] = createGroup("lateGroup", start = 3, end = 8)
		groupIds["departureGroup"] = createGroup("departureGroup", start = -5, end = 3)
		groupIds["openGroup"] = createGroup("openGroup", start = -5, end = null)
		groupIds["hiddenGroup"] = createGroup("hiddenGroup", start = 3, end = 3, visible = false)
	}

	private fun createArrivalParticipants() {
		create("ownArrival", start = 3)
		create("ownArrivalInside", start = 3, arrived = true)
		create("ownArrivalAfterTempOut", start = 3, out = true)
		create("ownArrivalHidden", start = 3, visible = false)
		create("otherDay", start = 4, end = 4)
		create("guestArrival", start = 3, type = GUEST)
		create("groupArrival", groups = listOf("arrivalGroup"))
		create("groupArrivalOwnDate", start = 4, groups = listOf("arrivalGroup"))
		create("groupArrivalInside", groups = listOf("arrivalGroup"), arrived = true)
		create("earliestGroupWins", groups = listOf("earlyGroup", "lateGroup"))
		create("hiddenGroupMember", groups = listOf("hiddenGroup"))
	}

	private fun createDepartureParticipants() {
		create("ownDeparture1", end = 3, arrived = true)
		create("ownDeparture2", end = 3, arrived = true)
		create("ownDepartureNeverArrived", end = 3)
		create("ownDepartureDeparted", end = 3, departed = true)
		create("guestDeparture", end = 3, type = GUEST)
		create("groupDeparture", start = -2, groups = listOf("departureGroup"), arrived = true)
		create("groupDepartureOut", groups = listOf("departureGroup"), out = true)
		create("groupDepartureOpen", groups = listOf("departureGroup", "openGroup"), arrived = true)
		create("groupDepartureOwnEnd", end = 4, groups = listOf("departureGroup"), arrived = true)
	}

	private fun ownNames(type: MovementTypeEnum): List<String> {
		val found = participantPort.findPlannedOnOwnDate(tenantId, type, day, 200).collectList().block()!!
		return found.map { it.lastName!! }
	}

	private fun groupNames(type: MovementTypeEnum): Map<String, UUID> {
		val found = participantPort.findPlannedOnGroupDate(tenantId, type, day, 200).collectList().block()!!
		return found.associate { it.second.lastName!! to it.first }
	}

	private fun createGroup(name: String, start: Long?, end: Long?, visible: Boolean = true): UUID {
		val group = GroupModel().apply {
			this.name = name
			this.visible = visible
			startAvailability = start?.let { CustomDateTimeModel(day.plusDays(it - 3)) }
			endAvailability = end?.let { CustomDateTimeModel(day.plusDays(it - 3)) }
			projectId = tenantId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		return groupPort.create(group).block()!!.id!!
	}

	private fun create(
		name: String,
		start: Long? = null,
		end: Long? = null,
		groups: List<String> = emptyList(),
		type: ParticipantTypeEnum = REGISTERED,
		visible: Boolean = true,
		arrived: Boolean = false,
		out: Boolean = false,
		departed: Boolean = false,
	) {
		val id = createParticipant(name, start, end, groups, type, visible)
		ids[name] = id
		if (arrived || out || departed) createMovement(id, IN, null)
		if (out) createMovement(id, OUT, SHOPPING)
		if (departed) createMovement(id, OUT, DEFINITIVE_DEPARTURE)
	}

	private fun createParticipant(
		name: String,
		start: Long?,
		end: Long?,
		groups: List<String>,
		participantType: ParticipantTypeEnum,
		isVisible: Boolean,
	): UUID {
		val participant = ParticipantModel().apply {
			firstName = "planned"
			lastName = name
			birthday = LocalDate.EPOCH
			type = participantType
			visible = isVisible
			startAvailability = start?.let { CustomDateTimeModel(day.plusDays(it - 3)) }
			endAvailability = end?.let { CustomDateTimeModel(day.plusDays(it - 3)) }
			this.groups = groups.map { GroupModel().apply { id = groupIds.getValue(it) } }
			projectId = tenantId
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
			projectId = tenantId
			create(currentUser(), ZonedDateTime.now(ZoneOffset.UTC))
		}
		movementPort.create(movement).block()
	}
}
