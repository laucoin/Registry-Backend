package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.HIGH
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class AlertLastCommunicationRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var alertPort: IAlertPort

	private val projectId = UUID.randomUUID()
	private val communicatedAlertId = UUID.randomUUID()
	private val silentAlertId = UUID.randomUUID()

	@BeforeEach
	fun setup() {
		databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, :name)")
			.bind("id", projectId).bind("name", "Alert last communication").then().block()
		insertAlert(communicatedAlertId, "communicated", ageInHours = 30)
		insertAlert(silentAlertId, "silent", ageInHours = 1)
		insertCommunication(communicatedAlertId, "first", ageInHours = 20, visible = true)
		insertCommunication(communicatedAlertId, "latest", ageInHours = 10, visible = true)
		insertCommunication(communicatedAlertId, "hidden", ageInHours = 2, visible = false)
	}

	@AfterEach
	fun cleanup() {
		databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", projectId).then().block()
	}

	private fun insertAlert(id: UUID, title: String, ageInHours: Long) {
		databaseClient.sql(
			"""
            INSERT INTO tb_alert (id, title, status, project_id, date_time)
            VALUES (:id, :title, 'IN_PROGRESS', :projectId, CURRENT_TIMESTAMP - (:age * INTERVAL '1 hour'))
            """
		).bind("id", id).bind("title", title).bind("projectId", projectId).bind("age", ageInHours).then().block()
	}

	private fun insertCommunication(alertId: UUID, message: String, ageInHours: Long, visible: Boolean) {
		databaseClient.sql(
			"""
            INSERT INTO tb_communication (id, message, alert_id, project_id, visible, date_time)
            VALUES (uuid_generate_v4(), :message, :alertId, :projectId, :visible, CURRENT_TIMESTAMP - (:age * INTERVAL '1 hour'))
            """
		).bind("message", message).bind("alertId", alertId).bind("projectId", projectId)
			.bind("visible", visible).bind("age", ageInHours).then().block()
	}

	private fun findAlertById(id: UUID): AlertModel = alertPort.findById(projectId, id, null).block()!!

	@Test
	fun `Should findById return the latest visible communication`() {
		// Arrange
		val id = communicatedAlertId

		// Act
		val result = findAlertById(id)

		// Assert
		assertEquals("latest", result.lastCommunication?.message)
	}

	@Test
	fun `Should findById return no last communication when the alert has none`() {
		// Arrange
		val id = silentAlertId

		// Act
		val result = findAlertById(id)

		// Assert
		assertNull(result.lastCommunication)
	}

	@Test
	fun `Should findPage return the last communication of each alert`() {
		// Arrange
		val params = AlertSearchParamModel(query = null)

		// Act
		val result = alertPort.findPage(projectId, PageableModel(0, 50), params).block()!!.content

		// Assert
		val messages = result.associate { it.title to it.lastCommunication?.message }
		assertEquals(mapOf<String?, String?>("communicated" to "latest", "silent" to null), messages)
	}

	@Test
	fun `Should update return the alert with its computed criticality and last communication`() {
		// Arrange
		val alert = findAlertById(communicatedAlertId).apply { title = "renamed" }

		// Act
		val result = alertPort.update(alert).block()!!

		// Assert
		assertEquals("renamed", result.title)
		assertEquals(AlertStatusEnum.IN_PROGRESS, result.status)
		assertEquals(HIGH, result.criticality)
		assertEquals("latest", result.lastCommunication?.message)
	}
}
