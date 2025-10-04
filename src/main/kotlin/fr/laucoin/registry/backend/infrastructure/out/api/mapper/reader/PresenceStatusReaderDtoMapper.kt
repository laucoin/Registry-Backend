package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class PresenceStatusReaderDtoMapper(
	private val translateService: ITranslateService,
): GenericDurationReaderDtoMapper(translateService) {
	fun toDto(
		model: PresenceStatusEnum,
		locale: Locale,
	): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model", locale = locale),
		)
	}

	fun toDto(
		model: PresenceStatusEnum,
		locale: Locale,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(
			model.name,
			extractLabelDuration(model, lastMovement, startAvailability, endAvailability, locale),
		)
	}

	private fun extractLabelDuration(
		model: PresenceStatusEnum,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
		locale: Locale
	): String {
		val now = CustomDateTimeModel.now()
		return when {
			Objects.nonNull(lastMovement) && listOf(IN, OUT).contains(model) -> {
				val interval = Duration.between(lastMovement, now.toZonedDateTime())

				translateService.getMessage(
					code = "$PRESENCE_STATUS_DURATION_PREFIX$model",
					args = arrayOf(formatDuration(interval, locale)),
					locale = locale,
				)
			}

			now.asStartIsAfterOther(startAvailability) -> {
				val interval = Duration.between(
					now.toZonedDateTime(),
					startAvailability?.toZonedDateTime(
						LocalTime.MIN, now.zone()!!
					)
				)

				translateService.getMessage(
					code = "${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE",
					args = arrayOf(formatDuration(interval, locale)),
					locale = locale,
				)
			}

			now.asEndIsBeforeOther(endAvailability) -> {
				val interval = Duration.between(
					endAvailability?.toZonedDateTime(
						LocalTime.MAX, now.zone()!!
					), now.toZonedDateTime()
				)

				translateService.getMessage(
					code = "${PRESENCE_STATUS_DURATION_PREFIX}LEFT",
					args = arrayOf(formatDuration(interval, locale)),
					locale = locale,
				)
			}

			Objects.isNull(lastMovement) -> {
				translateService.getMessage(
					code = "${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET",
					locale = locale
				)
			}

			else -> translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model", locale = locale)
		}
	}
}
