package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.HIGH
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.RECENT
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.REINFORCED
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.test.TestContext
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.r2dbc.core.DatabaseClient

@Import(QueryClockRepositoryTest.FixedClockConfig::class)
class QueryClockRepositoryTest : TestContext() {
	@TestConfiguration
	class FixedClockConfig {
		@Bean
		@Primary
		fun fixedClock(): Clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
	}

	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Autowired
	private lateinit var alertPort: IAlertPort

	private val projectId = UUID.randomUUID()
	private val alertIds = mapOf("recent" to UUID.randomUUID(), "reinforced" to UUID.randomUUID(), "high" to UUID.randomUUID())

	private companion object {
		val FIXED_NOW: Instant = Instant.parse("2031-03-05T12:00:00Z")
		val AGES_IN_HOURS = mapOf("recent" to 1L, "reinforced" to 7L, "high" to 19L)
	}

	@BeforeEach
	fun setup() {
		databaseClient.sql("INSERT INTO tb_project (id, name) VALUES (:id, :name)")
			.bind("id", projectId).bind("name", "Query clock").then().block()
		alertIds.forEach { (title, id) -> insertAlert(id, title) }
	}

	@AfterEach
	fun cleanup() {
		databaseClient.sql("DELETE FROM tb_project WHERE id = :id").bind("id", projectId).then().block()
	}

	private fun insertAlert(id: UUID, title: String) {
		val dateTime = FIXED_NOW.minusSeconds(AGES_IN_HOURS.getValue(title) * 3600).atOffset(ZoneOffset.UTC)
		databaseClient.sql(
			"""
            INSERT INTO tb_alert (id, title, status, project_id, date_time)
            VALUES (:id, :title, 'IN_PROGRESS', :projectId, :dateTime)
            """
		).bind("id", id).bind("title", title).bind("projectId", projectId).bind("dateTime", dateTime).then().block()
	}

	@Test
	fun `Should compute the alert criticality from the application clock instead of the database clock`() {
		// Arrange
		val expected = mapOf("recent" to RECENT, "reinforced" to REINFORCED, "high" to HIGH)

		// Act
		val actual = alertIds.mapValues { alertPort.findById(projectId, it.value, null).block()!!.criticality }

		// Assert
		assertEquals(expected, actual)
	}
}
