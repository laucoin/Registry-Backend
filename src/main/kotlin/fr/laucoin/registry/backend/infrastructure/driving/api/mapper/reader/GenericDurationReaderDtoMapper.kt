package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.Duration

abstract class GenericDurationReaderDtoMapper(
	private val translateService: ITranslateService,
) {
	private companion object {
		const val DAYS_PER_WEEK = 7L
		const val DAYS_PER_MONTH = 30L
		const val DAYS_PER_YEAR = 365L
		const val MONTHS_PER_YEAR = 12L
		const val HOURS_PER_DAY = 24L
		const val MINUTES_PER_HOUR = 60L
	}

	fun formatDuration(duration: Duration): String {
		val days = duration.toDays()
		return when {
			days >= DAYS_PER_YEAR -> formatYears(days)

			days >= DAYS_PER_MONTH -> {
				val months = days / DAYS_PER_MONTH
				join("month", months, "week", (days - months * DAYS_PER_MONTH) / DAYS_PER_WEEK)
			}

			days >= DAYS_PER_WEEK -> join("week", days / DAYS_PER_WEEK, "day", days % DAYS_PER_WEEK)
			days >= 1 -> join("day", days, "hour", duration.toHours() % HOURS_PER_DAY)
			duration.toHours() >= 1 -> join("hour", duration.toHours(), "minute", duration.toMinutes() % MINUTES_PER_HOUR)
			else -> translateService.getMessage(code = "${DURATION_PREFIX}less-than-an-hour")
		}
	}

	private fun formatYears(days: Long): String {
		val years = days / DAYS_PER_YEAR
		val months = (days - years * DAYS_PER_YEAR) / DAYS_PER_MONTH
		return when (months) {
			MONTHS_PER_YEAR -> join("year", years + 1, "month", 0)
			else -> join("year", years, "month", months)
		}
	}

	private fun join(majorUnit: String, major: Long, minorUnit: String, minor: Long): String {
		val majorText = unit(majorUnit, major)
		if (minor == 0L) return majorText

		return translateService.getMessage(
			code = "${DURATION_PREFIX}and",
			args = arrayOf(majorText, unit(minorUnit, minor)),
		)
	}

	private fun unit(unit: String, count: Long): String {
		return when (count) {
			1L -> translateService.getMessage(code = "$DURATION_PREFIX$unit")
			else -> translateService.getMessage(code = "$DURATION_PREFIX${unit}s", args = arrayOf(count))
		}
	}
}
