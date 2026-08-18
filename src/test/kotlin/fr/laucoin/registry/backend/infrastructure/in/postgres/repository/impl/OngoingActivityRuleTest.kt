package fr.laucoin.registry.backend.infrastructure.`in`.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.test.ModelExt.activityId
import fr.laucoin.registry.backend.test.ModelExt.projectId
import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The three cases that decide whether the safety board keeps an outing: a stay
 * expiring is not a return, a definitive departure is, and two records saved for
 * the same instant must resolve to the one entered last rather than to whichever
 * the planner happened to emit.
 */
class OngoingActivityRuleTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var repository: IMovementPort

	private val recorded: MutableList<UUID> = mutableListOf()
	private lateinit var participant: UUID
	private lateinit var outing: UUID

	@BeforeEach
	fun createOuting() {
		participant = UUID.randomUUID()
		outing = UUID.randomUUID()
		execute(
			"""
            INSERT INTO tb_participant (id, first_name, last_name, birthday, project_id, type)
            VALUES ('$participant', 'Ongoing', 'FIXTURE', DATE '1990-01-01', '$projectId', 'REGISTERED')
            """
		)
		recordMovement(outing, "OUT", "CURRENT_TIMESTAMP - INTERVAL '2 hours'", activityId)
	}

	@AfterEach
	fun removeOuting() {
		recorded.forEach { execute("DELETE FROM tb_movement_content WHERE movement_id = '$it'") }
		recorded.forEach { execute("DELETE FROM tb_movement WHERE id = '$it'") }
		execute("DELETE FROM tb_participant WHERE id = '$participant'")
		recorded.clear()
	}

	private fun recordMovement(id: UUID, type: String, dateTime: String, activity: UUID?) {
		recorded.add(id)
		execute(
			"""
            INSERT INTO tb_movement (id, date_time, type, activity_id, project_id)
            VALUES ('$id', $dateTime, '$type', ${activity?.let { "'$it'" } ?: "NULL"}, '$projectId')
            """
		)
		execute(
			"""
            INSERT INTO tb_movement_content (movement_id, participant_id)
            VALUES ('$id', '$participant')
            """
		)
	}

	private fun execute(sql: String) {
		databaseClient.sql(sql).fetch().rowsUpdated().block()
	}

	private fun listsOuting(): Boolean {
		val result = repository.findOngoingActivities(projectId, limit = 200).collectList().block()!!
		return result.any { it.id == outing }
	}

	@Test
	fun `Should keep an outing listed once its participant window has expired`() {
		// Arrange
		execute("UPDATE tb_participant SET end_availability_date = CURRENT_DATE - 1 WHERE id = '$participant'")

		// Act
		val listed = listsOuting()

		// Assert
		assertTrue(listed)
	}

	@Test
	fun `Should drop an outing once its participant has definitively departed`() {
		// Arrange
		execute("UPDATE tb_participant SET departed_at = CURRENT_TIMESTAMP WHERE id = '$participant'")

		// Act
		val listed = listsOuting()

		// Assert
		assertFalse(listed)
	}

	@Test
	fun `Should let the entry recorded last win over an exit sharing its timestamp`() {
		// Arrange
		val sameInstant = "(SELECT date_time FROM tb_movement WHERE id = '$outing')"
		recordMovement(UUID.randomUUID(), "IN", sameInstant, activity = null)

		// Act
		val listed = listsOuting()

		// Assert
		assertFalse(listed)
	}

	@Test
	fun `Should list an outing whose participant is still out`() {
		// Act
		val listed = listsOuting()

		// Assert
		assertTrue(listed)
	}
}
