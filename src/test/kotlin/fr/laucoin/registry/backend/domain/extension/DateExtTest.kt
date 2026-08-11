package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.extension.DateExt.isEndInRange
import fr.laucoin.registry.backend.domain.extension.DateExt.isInRange
import fr.laucoin.registry.backend.domain.extension.DateExt.isStartInRange
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.stream.Stream

class DateExtTest {
	private companion object {
		private val TODAY = LocalDate.of(2025, 9, 21)
		private val NOW = ZonedDateTime.of(TODAY, LocalTime.of(19, 43, 0), ZoneOffset.UTC)
		private val PAST_DATE = CustomDateTimeModel(TODAY.minusDays(10))
		private val PAST_DATETIME = CustomDateTimeModel(NOW.minusDays(10))
		private val FUTURE_DATE = CustomDateTimeModel(TODAY.plusDays(10))
		private val FUTURE_DATETIME = CustomDateTimeModel(NOW.plusDays(10))
		private val TODAY_DATE = CustomDateTimeModel(TODAY)
		private val NOW_DATETIME = CustomDateTimeModel(NOW)

		@JvmStatic
		fun `Should asStartIsAfterOther return true if start is after other`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(FUTURE_DATETIME, PAST_DATETIME, true),
				Arguments.of(FUTURE_DATE, PAST_DATETIME, true),
				Arguments.of(FUTURE_DATETIME, PAST_DATE, true),
				Arguments.of(FUTURE_DATETIME, FUTURE_DATE, true),
				Arguments.of(FUTURE_DATETIME, null, true),
				Arguments.of(FUTURE_DATE, null, true),
				Arguments.of(PAST_DATETIME, null, true),
				Arguments.of(PAST_DATE, null, true),
				Arguments.of(null, null, false),
				Arguments.of(PAST_DATETIME, FUTURE_DATETIME, false),
				Arguments.of(PAST_DATETIME, FUTURE_DATE, false),
				Arguments.of(PAST_DATE, FUTURE_DATETIME, false),
				Arguments.of(FUTURE_DATE, FUTURE_DATETIME, false),
				Arguments.of(null, FUTURE_DATETIME, false),
				Arguments.of(null, FUTURE_DATE, false),
				Arguments.of(null, PAST_DATETIME, false),
				Arguments.of(null, PAST_DATE, false),
			)

		@JvmStatic
		fun `Should asEndIsBeforeOther return true if start is before other`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(FUTURE_DATETIME, PAST_DATETIME, false),
				Arguments.of(FUTURE_DATE, PAST_DATETIME, false),
				Arguments.of(FUTURE_DATETIME, PAST_DATE, false),
				Arguments.of(FUTURE_DATETIME, FUTURE_DATE, true),
				Arguments.of(FUTURE_DATETIME, null, true),
				Arguments.of(FUTURE_DATE, null, true),
				Arguments.of(PAST_DATETIME, null, true),
				Arguments.of(PAST_DATE, null, true),
				Arguments.of(null, null, false),
				Arguments.of(PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(FUTURE_DATE, FUTURE_DATETIME, false),
				Arguments.of(null, FUTURE_DATETIME, false),
				Arguments.of(null, FUTURE_DATE, false),
				Arguments.of(null, PAST_DATETIME, false),
				Arguments.of(null, PAST_DATE, false),
			)

		@JvmStatic
		fun `Should isInRange return true if challenger is included in range`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(TODAY_DATE, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(TODAY_DATE, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(TODAY_DATE, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(TODAY_DATE, PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(TODAY_DATE, PAST_DATETIME, null, true),
				Arguments.of(TODAY_DATE, null, FUTURE_DATETIME, true),
				Arguments.of(TODAY_DATE, null, null, true),
				Arguments.of(FUTURE_DATE, PAST_DATE, TODAY_DATE, false),
				Arguments.of(FUTURE_DATE, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(FUTURE_DATE, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(FUTURE_DATE, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(FUTURE_DATE, PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(PAST_DATE, TODAY_DATE, FUTURE_DATE, false),
				Arguments.of(PAST_DATE, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(PAST_DATE, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(PAST_DATE, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(PAST_DATE, PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(NOW_DATETIME, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(NOW_DATETIME, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(NOW_DATETIME, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(NOW_DATETIME, PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(NOW_DATETIME, PAST_DATETIME, null, true),
				Arguments.of(NOW_DATETIME, null, FUTURE_DATETIME, true),
				Arguments.of(NOW_DATETIME, null, null, true),
				Arguments.of(FUTURE_DATETIME, PAST_DATETIME, NOW_DATETIME, false),
				Arguments.of(FUTURE_DATETIME, PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(FUTURE_DATETIME, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(FUTURE_DATETIME, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(FUTURE_DATETIME, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(PAST_DATETIME, NOW_DATETIME, FUTURE_DATETIME, false),
				Arguments.of(PAST_DATETIME, PAST_DATETIME, FUTURE_DATETIME, true),
				Arguments.of(PAST_DATETIME, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(PAST_DATETIME, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(PAST_DATETIME, PAST_DATE, FUTURE_DATE, true),
			)

		@JvmStatic
		fun `Should isStartInRange read a bare date as its midnight`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(TODAY_DATE, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(TODAY_DATE, TODAY_DATE, TODAY_DATE, true),
				Arguments.of(FUTURE_DATE, PAST_DATE, FUTURE_DATETIME, true),
				Arguments.of(FUTURE_DATE, PAST_DATE, TODAY_DATE, false),
				Arguments.of(PAST_DATE, PAST_DATETIME, FUTURE_DATE, false),
				Arguments.of(NOW_DATETIME, TODAY_DATE, TODAY_DATE, true),
				Arguments.of(null, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(TODAY_DATE, null, null, true),
			)

		@JvmStatic
		fun `Should isEndInRange read a bare date as its last second`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(TODAY_DATE, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(TODAY_DATE, TODAY_DATE, TODAY_DATE, true),
				Arguments.of(FUTURE_DATE, PAST_DATE, FUTURE_DATETIME, false),
				Arguments.of(PAST_DATE, PAST_DATETIME, FUTURE_DATE, true),
				Arguments.of(PAST_DATE, TODAY_DATE, FUTURE_DATE, false),
				Arguments.of(NOW_DATETIME, TODAY_DATE, TODAY_DATE, true),
				Arguments.of(null, PAST_DATE, FUTURE_DATE, true),
				Arguments.of(TODAY_DATE, null, null, true),
			)

		@JvmStatic
		fun `Should resolve the boundaries of a bare date to midnight and the last second`(): Stream<Arguments> =
			Stream.of(
				Arguments.of(
					CustomDateTimeModel(TODAY).asStart(),
					ZonedDateTime.of(TODAY, LocalTime.MIN, ZoneOffset.UTC),
				),
				Arguments.of(
					CustomDateTimeModel(TODAY).asEnd(),
					ZonedDateTime.of(TODAY, LocalTime.of(23, 59, 59), ZoneOffset.UTC),
				),
				Arguments.of(CustomDateTimeModel(NOW).asStart(), NOW),
				Arguments.of(CustomDateTimeModel(NOW).asEnd(), NOW),
			)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should asStartIsAfterOther return true if start is after other`(
		start: CustomDateTimeModel?,
		other: CustomDateTimeModel?,
		expected: Boolean,
	) {
		// Act
		val result = start?.copy().asStartIsAfterOther(other?.copy())

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should asEndIsBeforeOther return true if start is before other`(
		end: CustomDateTimeModel?,
		other: CustomDateTimeModel?,
		expected: Boolean,
	) {
		// Act
		val result = end?.copy().asEndIsBeforeOther(other?.copy())

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isInRange return true if challenger is included in range`(
		challenger: CustomDateTimeModel?,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		expected: Boolean,
	) {
		// Act
		val result = challenger?.copy().isInRange(start?.copy(), end?.copy())

		// Assert
		assertEquals(expected, result)
	}

	/**
	 * A date-only challenger must stay date-only: the comparison defaults
	 * missing times to the OffsetTime MIN/MAX sentinels internally, and if it
	 * leaked them back the persisted availability time became 00:00:00+18
	 * (out of Postgres's offset range) → BadSqlGrammar on insert. The shared
	 * MIN/MAX sentinels must also stay untouched.
	 */
	@Test
	fun `Should isInRange not mutate its inputs (no time leaks in)`() {
		// Arrange
		val challenger = CustomDateTimeModel(LocalDate.of(2025, 9, 21))
		val start = CustomDateTimeModel(LocalDate.of(2025, 9, 1))
		val end = CustomDateTimeModel(LocalDate.of(2025, 9, 30))

		// Act
		challenger.isInRange(start, end)

		// Assert
		assertNull(challenger.time)
		assertNull(start.time)
		assertNull(end.time)
		assertNull(CustomDateTimeModel.MIN.time)
		assertNull(CustomDateTimeModel.MAX.time)
	}

	/**
	 * The boundary rule, stated as the two roles a bare date can play. Both ends
	 * carrying the SAME date is the case that matters: a one-day stay is legal,
	 * and it is legal because the arrival is read as that day's midnight while the
	 * departure is read as its 23:59:59 — never the other way round.
	 */
	@ParameterizedTest
	@MethodSource
	fun `Should isStartInRange read a bare date as its midnight`(
		challenger: CustomDateTimeModel?,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		expected: Boolean,
	) {
		// Act
		val result = challenger.isStartInRange(start, end)

		// Assert
		assertEquals(expected, result)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should isEndInRange read a bare date as its last second`(
		challenger: CustomDateTimeModel?,
		start: CustomDateTimeModel?,
		end: CustomDateTimeModel?,
		expected: Boolean,
	) {
		// Act
		val result = challenger.isEndInRange(start, end)

		// Assert
		assertEquals(expected, result)
	}

	@Test
	fun `Should accept an arrival and a departure sharing one date inside a one-day window`() {
		// Arrange
		val theDay = CustomDateTimeModel(TODAY)

		// Act
		val arrivalAccepted = theDay.isStartInRange(theDay, theDay)
		val departureAccepted = theDay.isEndInRange(theDay, theDay)

		// Assert
		assertEquals(true, arrivalAccepted)
		assertEquals(true, departureAccepted)
	}

	@Test
	fun `Should leave a bare date untouched when its boundary is computed`() {
		// Arrange
		val bare = CustomDateTimeModel(TODAY)

		// Act
		bare.asStart()
		bare.asEnd()

		// Assert
		assertNull(bare.time)
	}

	@ParameterizedTest
	@MethodSource
	fun `Should resolve the boundaries of a bare date to midnight and the last second`(
		boundary: ZonedDateTime,
		expected: ZonedDateTime,
	) {
		// Act + Assert
		assertEquals(expected, boundary)
	}
}
