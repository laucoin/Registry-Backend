package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityQueries.ACTIVITY_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationQueries.COMMUNICATION_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.PARTICIPANT_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.USER_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleQueries.VEHICLE_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.test.TestContext
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class TextSearchClausesTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private companion object {
		const val SEARCH_TEXT = "Jules RICHARD"

		val CLAUSES = mapOf(
			"activities" to ACTIVITY_TEXT_SEARCH_CLAUSE,
			"alerts" to ALERT_TEXT_SEARCH_CLAUSE,
			"communications" to COMMUNICATION_TEXT_SEARCH_CLAUSE,
			"participants" to PARTICIPANT_TEXT_SEARCH_CLAUSE,
			"users" to USER_TEXT_SEARCH_CLAUSE,
			"vehicles" to VEHICLE_TEXT_SEARCH_CLAUSE,
		)

		val QUERIES = listOf(
			null to true,
			"jules rich" to true,
			"rich" to true,
			"jles" to true,
			"rchard" to true,
			"jlz" to false,
			"rchrd" to false,
			"xyz" to false,
		)
	}

	@TestFactory
	fun `Should keep a text only above the word similarity threshold in every searched list`(): Stream<DynamicTest> {
		return CLAUSES.entries.stream().flatMap { (name, clause) ->
			QUERIES.stream().map { (query, expected) ->
				dynamicTest("$name searched with $query") {
					// Arrange
					val sql = "SELECT 1 FROM (SELECT CAST('$SEARCH_TEXT' AS TEXT) AS search_text) t WHERE $clause"
					val spec = databaseClient.sql(sql).let {
						if (query == null) it.bindNull("query", String::class.java) else it.bind("query", query)
					}

					// Act
					val result = spec.fetch().all().collectList().block()!!

					// Assert
					assertEquals(expected, result.isNotEmpty())
				}
			}
		}
	}
}
