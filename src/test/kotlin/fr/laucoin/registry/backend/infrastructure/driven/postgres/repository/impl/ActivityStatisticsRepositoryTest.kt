package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

@TestInstance(PER_CLASS)
class ActivityStatisticsRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var movementPort: IMovementPort

	private val projectId = UUID.randomUUID()
	private val activityId = UUID.randomUUID()
	private val otherActivityId = UUID.randomUUID()
	private val ongoingActivityId = UUID.randomUUID()
	private val first = UUID.randomUUID()
	private val second = UUID.randomUUID()
	private val third = UUID.randomUUID()
	private val hidden = UUID.randomUUID()
	private val idle = UUID.randomUUID()
	private val base = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, UTC)

	@BeforeAll
	fun setup() {
		execute("INSERT INTO tb_project (id, name) VALUES (:id, 'Activity statistics')", "id" to projectId)
		listOf(activityId, otherActivityId, ongoingActivityId).forEach {
			execute("INSERT INTO tb_activity (id, name, project_id) VALUES (:id, 'activity', :p)", "id" to it, "p" to projectId)
		}
		listOf(first, second, third, idle).forEach { insertParticipant(it) }
		insertParticipant(hidden, visible = false)
		insertParticipant(UUID.randomUUID(), type = "GUEST")
		insertDepartureAndReturns()
		insertMovement("OUT", null, 10, activityId, first)
		insertMovement("OUT", "DEFINITIVE_DEPARTURE", 12, null, first)
		insertMovement("OUT", null, 20, activityId, third, visible = false)
		insertMovement("OUT", null, 30, activityId, hidden)
		insertMovement("OUT", null, 40, otherActivityId, idle)
	}

	@AfterAll
	fun cleanup() {
		execute("DELETE FROM tb_project WHERE id = :id", "id" to projectId)
	}

	@Test
	fun `Should count the participants, the movement time and the man time of an activity`() {
		// Arrange

		// Act
		val statistics = movementPort.findStatisticsByActivityId(projectId, activityId).block()!!

		// Assert
		assertEquals(2, statistics.participantCount)
		assertEquals(4, statistics.projectParticipantCount)
		assertEquals(0.5, statistics.participantRate)
		assertEquals((4 + 2) * 3600L, statistics.movementSeconds)
		assertEquals((2 + 4 + 2) * 3600L, statistics.manSeconds)
	}

	@Test
	fun `Should count an ongoing outing until now`() {
		// Arrange
		insertMovementAt(OffsetDateTime.now(UTC).minusHours(2), ongoingActivityId, idle)

		// Act
		val statistics = movementPort.findStatisticsByActivityId(projectId, ongoingActivityId).block()!!

		// Assert
		assertTrue(statistics.movementSeconds in 7_200..7_320)
		assertEquals(statistics.movementSeconds, statistics.manSeconds)
	}

	@Test
	fun `Should return zero for an activity nobody took part in`() {
		// Arrange
		val unusedActivity = UUID.randomUUID()
		execute("INSERT INTO tb_activity (id, name, project_id) VALUES (:id, 'unused', :p)", "id" to unusedActivity, "p" to projectId)

		// Act
		val statistics = movementPort.findStatisticsByActivityId(projectId, unusedActivity).block()!!

		// Assert
		assertEquals(0, statistics.participantCount)
		assertEquals(0, statistics.movementSeconds)
		assertEquals(0, statistics.manSeconds)
	}

	private fun insertDepartureAndReturns() {
		val departure = insertMovement("OUT", null, 1, activityId, first)
		execute("INSERT INTO tb_movement_content (movement_id, participant_id) VALUES (:m, :s)", "m" to departure, "s" to second)
		insertMovement("IN", null, 3, null, first)
		insertMovement("IN", null, 5, null, second)
	}

	private fun insertParticipant(id: UUID, visible: Boolean = true, type: String = "REGISTERED") {
		execute(
			"INSERT INTO tb_participant (id, first_name, last_name, birthday, type, project_id, visible) VALUES (:id, 'stat', 'participant', CURRENT_DATE, :type, :p, :visible)",
			"id" to id, "type" to type, "p" to projectId, "visible" to visible,
		)
	}

	private fun insertMovement(type: String, reason: String?, hour: Long, activity: UUID?, participant: UUID, visible: Boolean = true): UUID {
		return insertMovementAt(base.plusHours(hour), activity, participant, type, reason, visible)
	}

	private fun insertMovementAt(
		at: OffsetDateTime,
		activity: UUID?,
		participant: UUID,
		type: String = "OUT",
		reason: String? = null,
		visible: Boolean = true,
	): UUID {
		val id = UUID.randomUUID()
		execute(
			"INSERT INTO tb_movement (id, date_time, type, reason, activity_id, project_id, visible) VALUES (:id, :at, :type, NULLIF(:reason, ''), CAST(NULLIF(:a, '') AS uuid), :p, :visible)",
			"id" to id, "at" to at, "type" to type, "reason" to (reason ?: ""), "a" to (activity?.toString() ?: ""), "p" to projectId, "visible" to visible,
		)
		execute("INSERT INTO tb_movement_content (movement_id, participant_id) VALUES (:m, :s)", "m" to id, "s" to participant)
		return id
	}

	private fun execute(sql: String, vararg binds: Pair<String, Any>) {
		binds.fold(databaseClient.sql(sql)) { spec, (name, value) -> spec.bind(name, value) }.then().block()
	}
}
