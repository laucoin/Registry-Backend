package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.DEFINITIVE_DEPARTURE
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

@TestInstance(PER_CLASS)
class MovementEventRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var movementPort: IMovementPort

	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()
	private val participantId = UUID.randomUUID()
	private val otherParticipantId = UUID.randomUUID()
	private val driverId = UUID.randomUUID()
	private val vehicleId = UUID.randomUUID()
	private val base = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, UTC)
	private lateinit var definitiveDepartureId: UUID

	@BeforeAll
	fun setup() {
		listOf(projectId, otherProjectId).forEach {
			execute("INSERT INTO tb_project (id, name) VALUES (:id, 'Movement events')", "id" to it)
		}
		listOf(participantId, otherParticipantId, driverId).forEach { insertParticipant(it) }
		insertMovement("OUT", "SHOPPING", 1, participantId)
		insertMovement("IN", null, 3, participantId)
		insertMovement("OUT", "MEDICAL", 4, participantId, visible = false)
		definitiveDepartureId = insertMovement("OUT", "DEFINITIVE_DEPARTURE", 6, participantId)
		insertMovement("OUT", "SHOPPING", 2, otherParticipantId)
		insertVehicle()
		insertMovement("OUT", "SHOPPING", 1, driverId, vehicle = vehicleId)
		insertMovement("IN", null, 5, driverId, vehicle = vehicleId)
		insertMovement("OUT", "OTHER", 7, driverId, visible = false, vehicle = vehicleId)
	}

	@AfterAll
	fun cleanup() {
		listOf(projectId, otherProjectId).forEach { execute("DELETE FROM tb_project WHERE id = :id", "id" to it) }
	}

	@Test
	fun `Should find the visible movements of a participant ordered by date`() {
		// Arrange

		// Act
		val events = movementPort.findEventsByParticipantId(projectId, participantId).collectList().block()!!

		// Assert
		assertEquals(listOf(OUT, IN, OUT), events.map { it.type })
		assertEquals(listOf(SHOPPING, null, DEFINITIVE_DEPARTURE), events.map { it.reason })
		assertEquals(listOf(base.plusHours(1), base.plusHours(3), base.plusHours(6)).map { it.toInstant() }, events.map { it.dateTime.toInstant() })
	}

	@Test
	fun `Should find nothing for a participant looked up in another project`() {
		// Arrange

		// Act
		val events = movementPort.findEventsByParticipantId(otherProjectId, participantId).collectList().block()!!

		// Assert
		assertTrue(events.isEmpty())
	}

	@Test
	fun `Should find the visible movements of a vehicle ordered by date`() {
		// Arrange

		// Act
		val events = movementPort.findEventsByVehicleId(projectId, vehicleId).collectList().block()!!

		// Assert
		assertEquals(listOf(OUT, IN), events.map { it.type })
		assertEquals(listOf(base.plusHours(1), base.plusHours(5)).map { it.toInstant() }, events.map { it.dateTime.toInstant() })
	}

	@Test
	fun `Should find nothing for a vehicle looked up in another project`() {
		// Arrange

		// Act
		val events = movementPort.findEventsByVehicleId(otherProjectId, vehicleId).collectList().block()!!

		// Assert
		assertTrue(events.isEmpty())
	}

	@Test
	fun `Should find a later visible movement of the participants`() {
		// Arrange
		val at = base.plusHours(5).toZonedDateTime()

		// Act
		val exists = movementPort.existsLaterByParticipantIds(projectId, listOf(participantId), at, UUID.randomUUID()).block()!!

		// Assert
		assertTrue(exists)
	}

	@Test
	fun `Should ignore the excluded movement and the hidden ones when looking for a later movement`() {
		// Arrange
		val at = base.plusHours(2).plusMinutes(30).toZonedDateTime()

		// Act
		val exists = movementPort.existsLaterByParticipantIds(projectId, listOf(participantId), base.plusHours(3).toZonedDateTime(), definitiveDepartureId).block()!!
		val earlier = movementPort.existsLaterByParticipantIds(projectId, listOf(participantId), at, definitiveDepartureId).block()!!

		// Assert
		assertFalse(exists)
		assertTrue(earlier)
	}

	@Test
	fun `Should find no later movement for participants looked up in another project or without participants`() {
		// Arrange
		val at = base.toZonedDateTime()

		// Act
		val otherProject = movementPort.existsLaterByParticipantIds(otherProjectId, listOf(participantId), at, UUID.randomUUID()).block()!!
		val noParticipant = movementPort.existsLaterByParticipantIds(projectId, emptyList(), at, UUID.randomUUID()).block()!!

		// Assert
		assertFalse(otherProject)
		assertFalse(noParticipant)
	}

	@Test
	fun `Should find a later movement without excluding any when creating`() {
		// Arrange
		val at = base.plusHours(5).toZonedDateTime()

		// Act
		val exists = movementPort.existsLaterByParticipantIds(projectId, listOf(participantId), at, null).block()!!
		val afterAll = movementPort.existsLaterByParticipantIds(projectId, listOf(participantId), base.plusHours(6).toZonedDateTime(), null).block()!!

		// Assert
		assertTrue(exists)
		assertFalse(afterAll)
	}

	@Test
	fun `Should find a visible departure of the participants within the range only`() {
		// Arrange
		val from = base.toZonedDateTime()

		// Act
		val within = movementPort.existsDepartureBetween(projectId, listOf(participantId), from, base.plusHours(1).toZonedDateTime()).block()!!
		val hiddenOnly = movementPort.existsDepartureBetween(projectId, listOf(participantId), base.plusHours(3).toZonedDateTime(), base.plusHours(5).toZonedDateTime()).block()!!
		val otherProject = movementPort.existsDepartureBetween(otherProjectId, listOf(participantId), from, base.plusHours(8).toZonedDateTime()).block()!!

		// Assert
		assertTrue(within)
		assertFalse(hiddenOnly)
		assertFalse(otherProject)
	}

	@Test
	fun `Should find a visible departure of the participants without lower bound`() {
		// Arrange
		val beforeAll = base.toZonedDateTime()

		// Act
		val untilFirst = movementPort.existsDepartureBetween(projectId, listOf(participantId), null, base.plusHours(1).toZonedDateTime()).block()!!
		val none = movementPort.existsDepartureBetween(projectId, listOf(participantId), null, beforeAll).block()!!

		// Assert
		assertTrue(untilFirst)
		assertFalse(none)
	}

	private fun insertParticipant(id: UUID) {
		execute(
			"INSERT INTO tb_participant (id, first_name, last_name, birthday, type, project_id) VALUES (:id, 'event', 'participant', CURRENT_DATE, 'REGISTERED', :p)",
			"id" to id, "p" to projectId,
		)
	}

	private fun insertVehicle() {
		execute(
			"INSERT INTO tb_vehicle (id, license_plate, brand, model, project_id) VALUES (:id, 'AA-111-AA', 'brand', 'model', :p)",
			"id" to vehicleId, "p" to projectId,
		)
	}

	private fun insertMovement(
		type: String,
		reason: String?,
		hour: Long,
		participant: UUID,
		visible: Boolean = true,
		vehicle: UUID? = null,
	): UUID {
		val id = UUID.randomUUID()
		execute(
			"INSERT INTO tb_movement (id, date_time, type, reason, project_id, visible) VALUES (:id, :at, :type, NULLIF(:reason, ''), :p, :visible)",
			"id" to id, "at" to base.plusHours(hour), "type" to type, "reason" to (reason ?: ""), "p" to projectId, "visible" to visible,
		)
		execute(
			"INSERT INTO tb_movement_content (movement_id, participant_id, vehicle_id) VALUES (:m, :s, CAST(NULLIF(:v, '') AS uuid))",
			"m" to id, "s" to participant, "v" to (vehicle?.toString() ?: ""),
		)
		return id
	}

	private fun execute(sql: String, vararg binds: Pair<String, Any>) {
		binds.fold(databaseClient.sql(sql)) { spec, (name, value) -> spec.bind(name, value) }.then().block()
	}
}
