package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityQueries.ACTIVITY_DURATION_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityQueries.ACTIVITY_PARTICIPANTS_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityQueries.DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE
import fr.laucoin.registry.backend.test.TestContext
import java.time.ZonedDateTime
import java.time.ZonedDateTime.parse
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient

class ActivityQueriesTest: TestContext() {
	@Autowired
	private lateinit var databaseClient: DatabaseClient

	private companion object {
		const val WINDOW = """
            SELECT 1 FROM (
                SELECT DATE '2024-05-01' AS start_availability_date, TIME WITH TIME ZONE '10:00:00+02' AS start_availability_time,
                       DATE '2024-05-02' AS end_availability_date, TIME WITH TIME ZONE '10:00:00+02' AS end_availability_time
            ) t WHERE """

		const val CAPACITY = """
            SELECT 1 FROM (
                SELECT 'PT1H30M' AS duration, 5 AS min_allowed_participants, 10 AS max_allowed_participants
                UNION ALL SELECT '2h', NULL, NULL
            ) t WHERE """

		@JvmStatic
		fun `Should compare the instant with the window whatever its offset`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, true),
			Arguments.of(parse("2024-05-01T08:00:00Z"), true),
			Arguments.of(parse("2024-05-01T07:59:59Z"), false),
			Arguments.of(parse("2024-05-01T10:00:00+02:00"), true),
			Arguments.of(parse("2024-05-01T09:59:59+02:00"), false),
			Arguments.of(parse("2024-05-01T22:00:00-10:00"), true),
			Arguments.of(parse("2024-05-02T08:00:00Z"), true),
			Arguments.of(parse("2024-05-02T08:00:01Z"), false),
			Arguments.of(parse("2024-05-02T21:00:01+13:00"), false),
		)

		@JvmStatic
		fun `Should filter the activities by duration`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, null, 2),
			Arguments.of("PT1H30M", null, 2),
			Arguments.of("PT1H31M", null, 1),
			Arguments.of(null, "PT1H30M", 1),
			Arguments.of(null, "PT1H", 0),
			Arguments.of("PT1H", "PT2H", 2),
			Arguments.of("PT2H1M", null, 0),
		)

		@JvmStatic
		fun `Should filter the activities by hosted participants`(): Stream<Arguments> = Stream.of(
			Arguments.of(null, 2),
			Arguments.of(4, 1),
			Arguments.of(5, 2),
			Arguments.of(10, 2),
			Arguments.of(11, 1),
		)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should compare the instant with the window whatever its offset`(at: ZonedDateTime?, expected: Boolean) {
		// Arrange
		val query = "$WINDOW $DATE_IN_ACTIVITY_DATES_RANGE_CLAUSE"
		val spec = databaseClient.sql(query).let {
			if (at == null) it.bindNull("at", ZonedDateTime::class.java) else it.bind("at", at)
		}

		// Act
		val result = spec.fetch().all().collectList().block()!!

		// Assert
		assertEquals(expected, result.isNotEmpty())
	}

	@ParameterizedTest
	@MethodSource
	fun `Should filter the activities by duration`(minDuration: String?, maxDuration: String?, expectedCount: Int) {
		// Arrange
		val spec = databaseClient.sql("$CAPACITY $ACTIVITY_DURATION_CLAUSE")
			.let { if (minDuration == null) it.bindNull("minDuration", String::class.java) else it.bind("minDuration", minDuration) }
			.let { if (maxDuration == null) it.bindNull("maxDuration", String::class.java) else it.bind("maxDuration", maxDuration) }

		// Act
		val result = spec.fetch().all().collectList().block()!!

		// Assert
		assertEquals(expectedCount, result.size)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should filter the activities by hosted participants`(participants: Int?, expectedCount: Int) {
		// Arrange
		val spec = databaseClient.sql("$CAPACITY $ACTIVITY_PARTICIPANTS_CLAUSE")
			.let { if (participants == null) it.bindNull("participants", Int::class.javaObjectType) else it.bind("participants", participants) }

		// Act
		val result = spec.fetch().all().collectList().block()!!

		// Assert
		assertEquals(expectedCount, result.size)
	}
}
