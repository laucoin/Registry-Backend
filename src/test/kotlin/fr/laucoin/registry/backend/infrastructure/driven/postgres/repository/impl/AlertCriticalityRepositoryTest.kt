package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.HIGH
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.RECENT
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.REINFORCED
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.test.TestContext
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AlertCriticalityRepositoryTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var alertPort: IAlertPort

	private val projectId = UUID.randomUUID()
	private val pageable = PageableModel(0, 50)
	private val alertIds = AGES_IN_MINUTES.keys.associateWith { UUID.randomUUID() }

	private companion object {
		val AGES_IN_MINUTES = linkedMapOf(
			"recent" to 359L,
			"reinforced-lower-bound" to 361L,
			"reinforced-upper-bound" to 1079L,
			"high" to 1081L,
			"resolved" to 5000L,
		)
	}

	@BeforeEach
	fun setup() {
		databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, :name)")
			.bind("id", projectId).bind("name", "Alert criticality").then().block()
		alertIds.forEach { (title, id) -> insertAlert(id, title, AGES_IN_MINUTES.getValue(title)) }
	}

	@AfterEach
	fun cleanup() {
		databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", projectId).then().block()
	}

	private fun insertAlert(id: UUID, title: String, ageInMinutes: Long) {
		val status = if (title == "resolved") AlertStatusEnum.RESOLVED else AlertStatusEnum.IN_PROGRESS
		databaseClient.sql(
			"""
            INSERT INTO tb_alert (id, title, status, project_id, date_time)
            VALUES (:id, :title, :status, :projectId, CURRENT_TIMESTAMP - (:age * INTERVAL '1 minute'))
            """
		).bind("id", id).bind("title", title).bind("status", status.name)
			.bind("projectId", projectId).bind("age", ageInMinutes).then().block()
	}

	private fun findTitles(criticality: AlertCriticalityEnum? = null, sort: SortModel? = null): List<String?> {
		val params = AlertSearchParamModel(query = null, criticality = criticality, sort = sort)
		return alertPort.findPage(projectId, pageable, params).block()!!.content.map(AlertModel::title)
	}

	@Test
	fun `Should findById compute the criticality of an in-progress alert from its age`() {
		// Arrange
		val expected = mapOf("recent" to RECENT, "reinforced-lower-bound" to REINFORCED, "high" to HIGH)

		// Act
		val actual = expected.keys.associateWith { alertPort.findById(projectId, alertIds.getValue(it), null).block()!!.criticality }

		// Assert
		assertEquals(expected, actual)
	}

	@Test
	fun `Should findById give no criticality to a resolved alert`() {
		// Arrange
		val resolvedId = alertIds.getValue("resolved")

		// Act
		val actual = alertPort.findById(projectId, resolvedId, null).block()!!

		// Assert
		assertNull(actual.criticality)
	}

	@Test
	fun `Should findPage keep only the alerts of the requested criticality`() {
		// Arrange
		val expected = mapOf(
			RECENT to listOf("recent"),
			REINFORCED to listOf("reinforced-lower-bound", "reinforced-upper-bound"),
			HIGH to listOf("high"),
		)

		// Act
		val actual = expected.keys.associateWith { findTitles(criticality = it) }

		// Assert
		assertEquals(expected, actual)
	}

	@Test
	fun `Should findPage sort by criticality with the most critical first when descending`() {
		// Arrange
		val sort = SortModel("criticality", isDescending = true)

		// Act
		val actual = findTitles(sort = sort)

		// Assert
		assertEquals(listOf("high", "reinforced-upper-bound", "reinforced-lower-bound", "recent", "resolved"), actual)
	}

	@Test
	fun `Should findPage sort by criticality with the least critical first when ascending`() {
		// Arrange
		val sort = SortModel("criticality", isDescending = false)

		// Act
		val actual = findTitles(sort = sort)

		// Assert
		assertEquals(listOf("recent", "reinforced-lower-bound", "reinforced-upper-bound", "high", "resolved"), actual)
	}
}
