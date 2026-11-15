package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class AlertStatisticsRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var alertPort: IAlertPort

	private val projectId = UUID.randomUUID()
	private val otherProjectId = UUID.randomUUID()
	private val createdAt = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, UTC)

	@BeforeEach
	fun setup() {
		listOf(projectId, otherProjectId).forEach {
			databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, :name)")
				.bind("id", it).bind("name", "Alert statistics $it").then().block()
		}
	}

	@AfterEach
	fun cleanup() {
		listOf(projectId, otherProjectId).forEach {
			databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", it).then().block()
		}
	}

	private fun insertAlert(
		status: String,
		modifiedAfterHours: Long,
		visible: Boolean = true,
		project: UUID = projectId,
	): UUID {
		val id = UUID.randomUUID()
		databaseClient.sql(
			"""
            INSERT INTO tb_alert (id, title, status, project_id, date_time, last_modified_date, visible)
            VALUES (:id, 'alert', :status, :projectId, :createdAt, :modifiedAt, :visible)
            """
		).bind("id", id).bind("status", status).bind("projectId", project).bind("createdAt", createdAt)
			.bind("modifiedAt", createdAt.plusHours(modifiedAfterHours)).bind("visible", visible).then().block()
		return id
	}

	private fun insertHistory(alertId: UUID, version: Int, status: String, modifiedAfterHours: Long) {
		databaseClient.sql(
			"""
            INSERT INTO tb_alert_history (alert_id, version, date_time, status, project_id, created_date, last_modified_date, visible)
            VALUES (:alertId, :version, :createdAt, :status, :projectId, :createdAt, :modifiedAt, TRUE)
            """
		).bind("alertId", alertId).bind("version", version).bind("status", status).bind("projectId", projectId)
			.bind("createdAt", createdAt).bind("modifiedAt", createdAt.plusHours(modifiedAfterHours)).then().block()
	}

	@Test
	fun `Should count closed alerts and average the resolution time from the first resolved version`() {
		// Arrange
		insertAlert("RESOLVED", modifiedAfterHours = 2)
		val editedAfterResolution = insertAlert("RESOLVED", modifiedAfterHours = 10)
		insertHistory(editedAfterResolution, 1, "IN_PROGRESS", 1)
		insertHistory(editedAfterResolution, 2, "RESOLVED", 4)
		insertAlert("CANCELED", modifiedAfterHours = 3)
		insertAlert("IN_PROGRESS", modifiedAfterHours = 5)

		// Act
		val statistics = alertPort.findStatistics(projectId).block()!!

		// Assert
		assertEquals(2, statistics.resolvedCount)
		assertEquals(1, statistics.canceledCount)
		assertEquals(10_800, statistics.averageResolutionSeconds)
		assertEquals(2.0 / 3.0, statistics.realAlertRate)
	}

	@Test
	fun `Should ignore hidden alerts and alerts of other projects`() {
		// Arrange
		insertAlert("RESOLVED", modifiedAfterHours = 2)
		insertAlert("RESOLVED", modifiedAfterHours = 20, visible = false)
		insertAlert("CANCELED", modifiedAfterHours = 3, project = otherProjectId)

		// Act
		val statistics = alertPort.findStatistics(projectId).block()!!

		// Assert
		assertEquals(1, statistics.resolvedCount)
		assertEquals(0, statistics.canceledCount)
		assertEquals(7_200, statistics.averageResolutionSeconds)
	}

	@Test
	fun `Should return empty statistics when the project has no alert`() {
		// Arrange
		insertAlert("IN_PROGRESS", modifiedAfterHours = 1)

		// Act
		val statistics = alertPort.findStatistics(projectId).block()!!

		// Assert
		assertEquals(0, statistics.resolvedCount)
		assertEquals(0, statistics.canceledCount)
		assertNull(statistics.averageResolutionSeconds)
		assertNull(statistics.realAlertRate)
	}
}
