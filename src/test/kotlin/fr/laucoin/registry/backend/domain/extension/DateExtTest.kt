package fr.laucoin.registry.backend.domain.extension

import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.extension.DateExt.isInRange
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

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
}
