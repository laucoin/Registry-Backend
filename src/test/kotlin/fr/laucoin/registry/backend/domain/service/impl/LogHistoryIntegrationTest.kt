package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.service.core.IAlertService
import fr.laucoin.registry.backend.domain.service.core.ICommunicationService
import fr.laucoin.registry.backend.domain.service.core.IMovementService
import fr.laucoin.registry.backend.test.TestContext
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class LogHistoryIntegrationTest : TestContext() {
	@Autowired
	private lateinit var alertService: IAlertService

	@Autowired
	private lateinit var communicationService: ICommunicationService

	@Autowired
	private lateinit var movementService: IMovementService

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private val oldDateTime = OffsetDateTime.of(2020, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
	private val newDateTime = oldDateTime.plusHours(1)
	private val projectId = UUID.randomUUID()
	private val editorId = UUID.randomUUID()
	private val editor = CurrentUserModel().apply { id = editorId; role = "USER_ADMINISTRATOR" }

	@BeforeEach
	fun seed() {
		execute("INSERT INTO tb_project(id, name) VALUES (:id, 'History')", "id" to projectId)
		execute(
			"INSERT INTO tb_user(id, oidc_id, type, first_name, last_name, email, role) VALUES (:id, :oidc, 'USER', 'History', 'Editor', :email, 'USER')",
			"id" to editorId, "oidc" to UUID.randomUUID(), "email" to "$editorId@history.test",
		)
	}

	@AfterEach
	fun clean() {
		execute("DELETE FROM tb_project WHERE id = :id", "id" to projectId)
		execute("DELETE FROM tb_user WHERE id = :id", "id" to editorId)
	}

	@Test
	fun `Should updateAlertById keep the previous title and date in the history`() {
		// Arrange
		val id = UUID.randomUUID()
		execute(
			"INSERT INTO tb_alert(id, date_time, title, status, project_id) VALUES (:id, :dateTime, 'Old title', 'IN_PROGRESS', :project)",
			"id" to id, "dateTime" to oldDateTime, "project" to projectId,
		)
		val updated = AlertModel(title = "New title", dateTime = newDateTime.toZonedDateTime()).apply { projectId = this@LogHistoryIntegrationTest.projectId }

		// Act
		alertService.updateAlertById(editor, projectId, id, updated).block()

		// Assert
		assertEquals("Old title", text("SELECT title FROM tb_alert_history WHERE alert_id = :id AND version = 1", id))
		assertEquals("New title", text("SELECT title FROM tb_alert WHERE id = :id", id))
	}

	@Test
	fun `Should updateCommunicationById keep the previous message in the history`() {
		// Arrange
		val alertId = UUID.randomUUID()
		val id = UUID.randomUUID()
		execute(
			"INSERT INTO tb_alert(id, date_time, title, status, project_id) VALUES (:id, :dateTime, 'Alert', 'IN_PROGRESS', :project)",
			"id" to alertId, "dateTime" to oldDateTime, "project" to projectId,
		)
		execute(
			"INSERT INTO tb_communication(id, date_time, alert_id, message, project_id) VALUES (:id, :dateTime, :alert, 'Old message', :project)",
			"id" to id, "dateTime" to oldDateTime, "alert" to alertId, "project" to projectId,
		)
		val updated = CommunicationModel(dateTime = newDateTime.toZonedDateTime(), message = "New message").apply { projectId = this@LogHistoryIntegrationTest.projectId }

		// Act
		communicationService.updateCommunicationById(editor, projectId, id, updated).block()

		// Assert
		assertEquals("Old message", text("SELECT message FROM tb_communication_history WHERE communication_id = :id AND version = 1", id))
		assertEquals("New message", text("SELECT message FROM tb_communication WHERE id = :id", id))
	}

	@Test
	fun `Should updateMovementById keep the previous date in the history`() {
		// Arrange
		val id = UUID.randomUUID()
		execute(
			"INSERT INTO tb_movement(id, date_time, type, project_id) VALUES (:id, :dateTime, 'IN', :project)",
			"id" to id, "dateTime" to oldDateTime, "project" to projectId,
		)
		val updated = MovementModel(dateTime = newDateTime.toZonedDateTime(), type = IN).apply { projectId = this@LogHistoryIntegrationTest.projectId }

		// Act
		movementService.updateMovementById(editor, projectId, id, updated, emptyList()).block()

		// Assert
		val sql = "SELECT COUNT(*) FROM tb_movement_history WHERE movement_id = :id AND version = 1 AND date_time = :dateTime"
		assertEquals(1L, databaseClient.sql(sql).bind("id", id).bind("dateTime", oldDateTime)
			.map { row -> row.get(0, Long::class.javaObjectType)!! }.one().block())
	}

	private fun execute(sql: String, vararg params: Pair<String, Any>) {
		var spec = databaseClient.sql(sql)
		params.forEach { (name, value) -> spec = spec.bind(name, value) }
		spec.fetch().rowsUpdated().block()
	}

	private fun text(sql: String, id: UUID): String = databaseClient.sql(sql).bind("id", id)
		.map { row -> row.get(0, String::class.java)!! }.one().block()!!
}
