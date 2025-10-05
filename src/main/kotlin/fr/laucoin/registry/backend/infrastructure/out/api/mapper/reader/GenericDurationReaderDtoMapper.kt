package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import fr.laucoin.registry.backend.domain.service.ITranslateService
import java.time.Duration

abstract class GenericDurationReaderDtoMapper(
	private val translateService: ITranslateService,
) {
	fun formatDuration(duration: Duration): String {
		return when {
			duration.seconds == 1L -> translateService.getMessage(code = "${DURATION_PREFIX}second")
			duration.seconds < 60 -> translateService.getMessage(
				code = "${DURATION_PREFIX}seconds",
				args = arrayOf(duration.seconds),
			)

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
