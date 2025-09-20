package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.Duration
import java.util.Locale
import org.springframework.stereotype.Component

@Component
class GenericDurationReaderDtoMapper(
	private val translateService: ITranslateService,
) {
	protected fun formatDuration(duration: Duration, locale: Locale): String {
		return when {
			duration.seconds == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}second", locale = locale)
			duration.seconds < 60 -> translateService.getMessage(
				code = "${DURATION_PREFIX}seconds",
				args = arrayOf(duration.seconds),
				locale = locale,
			)

			duration.toMinutes() == 1L -> translateService.getMessage(
				code = "${DURATION_PREFIX}minute",
				locale = locale,
			)

			duration.toMinutes() <= 60 -> translateService.getMessage(
				code = "${DURATION_PREFIX}minutes",
				args = arrayOf(duration.toMinutes()),
				locale = locale,
			)

			duration.toHours() == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}hour", locale = locale)
			duration.toHours() < 24 -> translateService.getMessage(
				code = "${DURATION_PREFIX}hours",
				args = arrayOf(duration.toHours()),
				locale = locale,
			)

			duration.toDays() == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}day", locale = locale)
			duration.toDays() < 31 -> translateService.getMessage(
				code = "${DURATION_PREFIX}days",
				args = arrayOf(duration.toDays()),
				locale = locale,
			)

			duration.toDays() < 61 -> translateService.getMessage(code = "${DURATION_PREFIX}month", locale = locale)
			duration.toDays() < 365 -> translateService.getMessage(
				code = "${DURATION_PREFIX}months",
				args = arrayOf((duration.toDays() / 30L).toInt()),
				locale = locale,
			)

			duration.toDays() < 730 -> translateService.getMessage(code = "${DURATION_PREFIX}year", locale = locale)
			else -> translateService.getMessage(
				code = "${DURATION_PREFIX}years",
				args = arrayOf((duration.toDays() / 365L).toInt()),
				locale = locale,
			)
		}
	}
}
