package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Objects

@Component
class PresenceStatusReaderDtoMapper(
	private val translateService: ITranslateService,
) : GenericDurationReaderDtoMapper(translateService) {
	fun toDto(model: PresenceStatusEnum): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model"),
		)
	}

	fun toDto(
		model: PresenceStatusEnum,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(
			model.name,
			extractLabelDuration(model, lastMovement, startAvailability, endAvailability),
		)
	}

	/**
	 * Same correction as the availability labels: "arrives in {0}" is only true
	 * while the window has yet to OPEN, and "left {0} ago" only once it has
	 * CLOSED. Both used the availability comparison helpers, which answer the
	 * opposite question, so a participant expected next week was announced as
	 * having left. Boundaries come from the model's own asStart/asEnd — midnight
	 * and 23:59:59 for a date-only window — and a recorded movement still wins
	 * over both: someone who has actually moved is described by that movement,
	 * not by a schedule.
	 */
	private fun extractLabelDuration(
		model: PresenceStatusEnum,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now().toZonedDateTime()
		val start = startAvailability?.asStart()
		val end = endAvailability?.asEnd()
		return when {
			Objects.nonNull(lastMovement) && listOf(IN, OUT).contains(model) -> translateService.getMessage(
				code = "$PRESENCE_STATUS_DURATION_PREFIX$model",
				args = arrayOf(formatDuration(Duration.between(lastMovement, now))),
			)

			start != null && start.isAfter(now) -> translateService.getMessage(
				code = "${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE",
				args = arrayOf(formatDuration(Duration.between(now, start))),
			)

			end != null && end.isBefore(now) -> translateService.getMessage(
				code = "${PRESENCE_STATUS_DURATION_PREFIX}LEFT",
				args = arrayOf(formatDuration(Duration.between(end, now))),
			)

			Objects.isNull(lastMovement) -> {
				translateService.getMessage(code = "${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET")
			}

			else -> translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model")
		}
	}
}
