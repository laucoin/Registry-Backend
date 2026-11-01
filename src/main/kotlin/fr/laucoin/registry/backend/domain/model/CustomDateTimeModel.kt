package fr.laucoin.registry.backend.domain.model

import java.time.Clock
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
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
	constructor(dateTime: ZonedDateTime): this(dateTime.toLocalDate(), dateTime.toOffsetDateTime().toOffsetTime())

	companion object {
		val MIN = CustomDateTimeModel(OffsetDateTime.MIN.toZonedDateTime())
		val MAX = CustomDateTimeModel(OffsetDateTime.MAX.toZonedDateTime())
		val EPOCH = CustomDateTimeModel(LocalDate.EPOCH)

		@JsonIgnore
		fun now(clock: Clock): CustomDateTimeModel {
			return CustomDateTimeModel(ZonedDateTime.now(clock.withZone(ZoneOffset.UTC)))
		}
	}

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

	@JsonIgnore
	private fun clampOffsetTimeToPostgresRange(time: OffsetTime): OffsetTime {
		val offsetSeconds = time.offset.totalSeconds

		// Postgres ne supporte qu'une plage de -14 à +14 heures pour les décalages
		// (OffsetTime.MIN/MAX sont hors de cette plage (p. ex. -18 heures))
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
