package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertQueries.ALERT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationQueries.COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupQueries.DATE_IN_GROUP_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementQueries.MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantQueries.DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileQueries.DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectQueries.DATE_IN_PROJECT_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle.VehicleQueries.DATE_IN_VEHICLE_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.test.TestContext
import java.time.ZonedDateTime
import java.time.ZonedDateTime.parse
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class DateClausesTest : TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private data class WindowResource(val name: String, val clause: String, val columns: List<String>, val extraJoin: String = "")

	private companion object {
		val AVAILABILITY = listOf("start_availability_date", "start_availability_time", "end_availability_date", "end_availability_time")
		const val GROUP_JOIN = "CROSS JOIN (SELECT CAST(NULL AS JSON) AS groups, CAST(NULL AS JSON) AS available_groups_on_date) fg"

		val WINDOWS = listOf(
			WindowResource("groups", DATE_IN_GROUP_DATES_RANGE_CLAUSE, AVAILABILITY),
			WindowResource("vehicles", DATE_IN_VEHICLE_DATES_RANGE_CLAUSE, AVAILABILITY),
			WindowResource("participants", DATE_IN_PARTICIPANT_DATES_RANGE_CLAUSE, AVAILABILITY, GROUP_JOIN),
			WindowResource("projects", DATE_IN_PROJECT_DATES_RANGE_CLAUSE, listOf("begin_date", "begin_time", "end_date", "end_time")),
			WindowResource("profiles", DATE_IN_PROJECT_PROFILE_DATES_RANGE_CLAUSE, listOf("start_access_date", "start_access_time", "end_access_date", "end_access_time")),
		)

		val INSTANTS: List<Pair<ZonedDateTime?, Boolean>> = listOf(
			null to true,
			parse("2024-05-01T08:00:00Z") to true,
			parse("2024-05-01T07:59:59Z") to false,
			parse("2024-05-01T10:00:00+02:00") to true,
			parse("2024-05-01T09:59:59+02:00") to false,
			parse("2024-05-01T22:00:00-10:00") to true,
			parse("2024-05-02T08:00:00Z") to true,
			parse("2024-05-02T08:00:01Z") to false,
			parse("2024-05-02T21:00:01+13:00") to false,
		)

		val EVENT_CLAUSES = mapOf(
			"alerts" to ALERT_DATE_IN_DATES_RANGE_CLAUSE,
			"communications" to COMMUNICATION_DATE_IN_DATES_RANGE_CLAUSE,
			"movements" to MOVEMENT_DATE_IN_DATES_RANGE_CLAUSE,
		)

		val PERIODS: List<Triple<ZonedDateTime?, ZonedDateTime?, Boolean>> = listOf(
			Triple(null, null, true),
			Triple(parse("2024-05-01T08:00:00Z"), null, true),
			Triple(parse("2024-05-01T08:00:01Z"), null, false),
			Triple(null, parse("2024-05-01T08:00:00Z"), true),
			Triple(null, parse("2024-05-01T07:59:59Z"), false),
			Triple(parse("2024-05-01T10:00:00+02:00"), parse("2024-05-01T10:00:00+02:00"), true),
			Triple(parse("2024-05-01T09:00:00+14:00"), parse("2024-05-02T00:00:00-12:00"), true),
			Triple(parse("2024-05-01T10:00:01+02:00"), parse("2024-05-02T00:00:00Z"), false),
		)
	}

	@TestFactory
	fun `Should compare the instant with every validity window whatever its offset`(): Stream<DynamicTest> {
		return WINDOWS.stream().flatMap { resource ->
			INSTANTS.stream().map { (at, expected) ->
				dynamicTest("${resource.name} at $at") {
					// Arrange
					val spec = databaseClient.sql(windowQuery(resource)).let {
						if (at == null) it.bindNull("at", ZonedDateTime::class.java) else it.bind("at", at)
					}

					// Act
					val result = spec.fetch().all().collectList().block()!!

					// Assert
					assertEquals(expected, result.isNotEmpty())
				}
			}
		}
	}

	@TestFactory
	fun `Should compare the date-time of an event with the requested period whatever its offset`(): Stream<DynamicTest> {
		return EVENT_CLAUSES.entries.stream().flatMap { (name, clause) ->
			PERIODS.stream().map { (from, to, expected) ->
				dynamicTest("$name from $from to $to") {
					// Arrange
					val sql = "SELECT 1 FROM (SELECT TIMESTAMP WITH TIME ZONE '2024-05-01 10:00:00+02' AS date_time) t WHERE $clause"
					val spec = databaseClient.sql(sql)
						.let { if (from == null) it.bindNull("from", ZonedDateTime::class.java) else it.bind("from", from) }
						.let { if (to == null) it.bindNull("to", ZonedDateTime::class.java) else it.bind("to", to) }

					// Act
					val result = spec.fetch().all().collectList().block()!!

					// Assert
					assertEquals(expected, result.isNotEmpty())
				}
			}
		}
	}

	private fun windowQuery(resource: WindowResource): String {
		val (startDate, startTime, endDate, endTime) = resource.columns
		return """
            SELECT 1 FROM (
                SELECT DATE '2024-05-01' AS $startDate, TIME WITH TIME ZONE '10:00:00+02' AS $startTime,
                       DATE '2024-05-02' AS $endDate, TIME WITH TIME ZONE '10:00:00+02' AS $endTime
            ) t ${resource.extraJoin} WHERE ${resource.clause}
        """
	}
}
