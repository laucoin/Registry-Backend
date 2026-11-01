package fr.laucoin.registry.backend.infrastructure.driven.postgres

import fr.laucoin.registry.backend.test.TestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class DatabaseLockTimeoutTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	@Test
	fun `Should every connection apply the configured lock timeout`() {
		// Arrange
		val query = databaseClient.sql("SHOW lock_timeout")

		// Act
		val result = query.map { row -> row.get(0, String::class.java)!! }.one().block()

		// Assert
		assertEquals("5s", result)
	}
}
