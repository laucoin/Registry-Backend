package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.Duration

abstract class GenericDurationReaderDtoMapper(
	private val translateService: ITranslateService,
) {
	/**
	 * Sub-minute durations are deliberately fuzzy ("a few seconds"): exact
	 * seconds would push clients to re-render every second.
	 *
	 * The duration is taken as a MAGNITUDE. Direction is carried by the message
	 * the caller picks ("since {0}" vs "in {0}"), never by the sign: a negative
	 * Duration used to fall through every threshold — `seconds < 60` is true for
	 * anything negative — and came out as "a few seconds", which is how a date
	 * three weeks in the future ended up reading "no longer available since a few
	 * seconds".
	 */
	fun formatDuration(interval: Duration): String {
		val duration = interval.abs()
		return when {
			duration.seconds < 60 -> translateService.getMessage(code = "${DURATION_PREFIX}seconds")

			duration.toMinutes() == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}minute")

			duration.toMinutes() <= 60 -> translateService.getMessage(
				code = "${DURATION_PREFIX}minutes",
				args = arrayOf(duration.toMinutes()),
			)

			duration.toHours() == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}hour")
			duration.toHours() < 24 -> translateService.getMessage(
				code = "${DURATION_PREFIX}hours",
				args = arrayOf(duration.toHours()),
			)

			duration.toDays() == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}day")
			duration.toDays() < 31 -> translateService.getMessage(
				code = "${DURATION_PREFIX}days",
				args = arrayOf(duration.toDays()),
			)

			duration.toDays() < 61 -> translateService.getMessage(code = "${DURATION_PREFIX}month")
			duration.toDays() < 365 -> translateService.getMessage(
				code = "${DURATION_PREFIX}months",
				args = arrayOf((duration.toDays() / 30L).toInt()),
			)

			duration.toDays() < 730 -> translateService.getMessage(code = "${DURATION_PREFIX}year")
			else -> translateService.getMessage(
				code = "${DURATION_PREFIX}years",
				args = arrayOf((duration.toDays() / 365L).toInt()),
			)
		}
	}
}
