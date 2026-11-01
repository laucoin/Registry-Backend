package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import java.time.Clock
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OVERDUE
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Objects

@Component
class PresenceStatusReaderDtoMapper(
	private val translateService: ITranslateService,
	private val clock: Clock,
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

	private fun extractLabelDuration(
		model: PresenceStatusEnum,
		lastMovement: ZonedDateTime?,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now(clock)
		return when {
			model == OVERDUE -> translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model")

			Objects.nonNull(lastMovement) && listOf(IN, OUT).contains(model) ->
				durationMessage("$PRESENCE_STATUS_DURATION_PREFIX$model", Duration.between(lastMovement, now.toZonedDateTime()))

			Objects.nonNull(startAvailability) && now.asStartIsAfterOther(startAvailability) ->
				durationMessage("${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE", untilStart(now, startAvailability))

			Objects.nonNull(endAvailability) && now.asEndIsBeforeOther(endAvailability) ->
				durationMessage("${PRESENCE_STATUS_DURATION_PREFIX}LEFT", sinceEnd(now, endAvailability))

			Objects.isNull(lastMovement) -> translateService.getMessage(code = "${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET")

			else -> translateService.getMessage(code = "$PRESENCE_STATUS_PREFIX$model")
		}
	}
}
