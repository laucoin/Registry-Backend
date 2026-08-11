package fr.laucoin.registry.backend.domain.model

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.Objects


data class CustomDateTimeModel(
	var date: LocalDate,
	var time: OffsetTime? = null,
) {
	constructor(dateTime: ZonedDateTime) : this(dateTime.toLocalDate(), dateTime.toOffsetDateTime().toOffsetTime())

	companion object {
		/**
		 * Unbounded sentinels, deliberately DATE-ONLY. Built from
		 * `OffsetDateTime.MIN`/`MAX` they carried ±18 h offsets — outside what
		 * Postgres accepts and, worse, enough to push "the beginning of time"
		 * across a day boundary, so a window open from the earliest representable
		 * date no longer contained that very date. Date-only, they resolve through
		 * the same boundary rule as any other bare date.
		 */
		val MIN = CustomDateTimeModel(LocalDate.MIN)
		val MAX = CustomDateTimeModel(LocalDate.MAX)
		val EPOCH = CustomDateTimeModel(LocalDate.EPOCH)

		/**
		 * The zone every date-only boundary is resolved in. Deliberately fixed:
		 * `OffsetTime.MIN`/`MAX` carry ±18 h offsets that Postgres cannot store, so
		 * clamping them to its ±14 h limit moved a "midnight" boundary fourteen
		 * hours into the previous day — the whole point of a boundary is that it
		 * does not drift with whoever asks for it.
		 */
		val BOUNDARY_ZONE: ZoneOffset = ZoneOffset.UTC

		val START_OF_DAY: LocalTime = LocalTime.MIN
		val END_OF_DAY: LocalTime = LocalTime.of(23, 59, 59)

		@JsonIgnore
		fun now(): CustomDateTimeModel {
			return CustomDateTimeModel(ZonedDateTime.now(ZoneId.of("UTC")))
		}
	}

	/**
	 * The instant this value means when it opens a window (an arrival): a stated
	 * time as given, a bare date at MIDNIGHT. Computation only — the boundary is
	 * never written back onto the model, never persisted and never serialized
	 * (`@JsonIgnore`), so a date-only arrival stays date-only everywhere the
	 * outside world can see it. Pair with [asEnd] rather than mixing the two:
	 * that asymmetry is what lets an arrival and a departure share one date and
	 * still describe a whole day.
	 */
	@JsonIgnore
	fun asStart(): ZonedDateTime = toZonedDateTime(START_OF_DAY, BOUNDARY_ZONE)!!

	/**
	 * The instant this value means when it closes a window (a departure): a
	 * stated time as given, a bare date at 23:59:59. Same computation-only
	 * contract as [asStart].
	 */
	@JsonIgnore
	fun asEnd(): ZonedDateTime = toZonedDateTime(END_OF_DAY, BOUNDARY_ZONE)!!

	@JsonIgnore
	fun toZonedDateTime(defaultTime: OffsetTime? = null): ZonedDateTime? {
		return when {
			Objects.isNull(date) -> null
			Objects.nonNull(time) -> time!!.atDate(date).toZonedDateTime()
			Objects.nonNull(defaultTime) -> date.atTime(clampOffsetTimeToPostgresRange(defaultTime!!)).toZonedDateTime()
			else -> date.atStartOfDay(ZoneId.of("UTC"))
		}
	}

	@JsonIgnore
	fun toZonedDateTime(localTime: LocalTime, zone: ZoneOffset = ZoneOffset.UTC): ZonedDateTime? {
		return when {
			Objects.isNull(date) -> null
			Objects.nonNull(time) -> time!!.atDate(date).toZonedDateTime()
			Objects.nonNull(localTime) -> date.atTime(clampOffsetTimeToPostgresRange(OffsetTime.of(localTime, zone)))
				.toZonedDateTime()

			else -> date.atStartOfDay(ZoneId.of("UTC"))
		}
	}

	/**
	 * Postgres only supports a range of -14 to +14 hours for offsets
	 * (OffsetTime.MIN/MAX are outside this range (e.g., -18 hours)).
	 */
	@JsonIgnore
	private fun clampOffsetTimeToPostgresRange(time: OffsetTime): OffsetTime {
		val offsetSeconds = time.offset.totalSeconds

		val minOffsetSeconds = ZoneOffset.ofHours(-14).totalSeconds
		val maxOffsetSeconds = ZoneOffset.ofHours(14).totalSeconds

		val clampedOffsetSeconds = when {
			offsetSeconds < minOffsetSeconds -> minOffsetSeconds
			offsetSeconds > maxOffsetSeconds -> maxOffsetSeconds
			else -> offsetSeconds
		}

		val clampedOffset = ZoneOffset.ofTotalSeconds(clampedOffsetSeconds)
		return time.withOffsetSameLocal(clampedOffset)
	}

	@JsonIgnore
	override fun toString(): String {
		return when {
			Objects.isNull(date) -> ""
			Objects.isNull(time) -> date.format(ISO_LOCAL_DATE)
			else -> toZonedDateTime()!!.format(ISO_ZONED_DATE_TIME)
		}
	}

	@JsonIgnore
	fun zone(): ZoneOffset? {
		return time?.offset
	}

	fun plusHours(hours: Long): CustomDateTimeModel {
		if (Objects.nonNull(time)) {
			time = time!!.plusHours(hours)
		}
		return this
	}
}
