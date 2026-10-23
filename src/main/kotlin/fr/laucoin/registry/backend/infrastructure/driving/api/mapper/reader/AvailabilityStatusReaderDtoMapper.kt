package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.AvailabilitySubjectEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import org.springframework.stereotype.Component
import java.util.Objects

@Component
class AvailabilityStatusReaderDtoMapper(
	private val translateService: ITranslateService,
) : GenericDurationReaderDtoMapper(translateService) {
	fun toDto(model: AvailabilityStatusEnum): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$AVAILABILITY_STATUS_PREFIX$model"),
		)
	}

	fun toDto(
		model: AvailabilityStatusEnum,
		subject: AvailabilitySubjectEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(
			model.name,
			extractLabelDuration(model, subject, startAvailability, endAvailability),
		)
	}

	private fun extractLabelDuration(
		model: AvailabilityStatusEnum,
		subject: AvailabilitySubjectEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now()
		val durationPrefix = "${subject.keyPrefix}$DURATION_PREFIX"
		return when {
			Objects.nonNull(startAvailability) && AvailabilityStatusEnum.AVAILABLE === model ->
				durationMessage("$durationPrefix$model", sinceStart(now, startAvailability))

			startAvailability.asStartIsAfterOther(now) ->
				durationMessage("${durationPrefix}NOT_YET", untilStart(now, startAvailability))

			endAvailability.asEndIsBeforeOther(now) ->
				durationMessage("${durationPrefix}NO_MORE", sinceEnd(now, endAvailability))

			else -> translateService.getMessage(code = "${subject.keyPrefix}$model")
		}
	}
}
