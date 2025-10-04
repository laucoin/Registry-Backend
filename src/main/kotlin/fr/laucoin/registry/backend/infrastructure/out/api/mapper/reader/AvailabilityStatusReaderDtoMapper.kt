package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import java.time.Duration
import java.time.LocalTime
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class AvailabilityStatusReaderDtoMapper(
	private val translateService: ITranslateService,
): GenericDurationReaderDtoMapper(translateService) {
	fun toDto(model: AvailabilityStatusEnum): LabelDto {
		return LabelDto(
			model.name,
			translateService.getMessage(code = "$AVAILABILITY_STATUS_PREFIX$model"),
		)
	}

	fun toDto(
		model: AvailabilityStatusEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(
			model.name,
			extractLabelDuration(model, startAvailability, endAvailability),
		)
	}

	private fun extractLabelDuration(
		model: AvailabilityStatusEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now()
		return when {
			Objects.nonNull(startAvailability) && AvailabilityStatusEnum.AVAILABLE === model -> {
				val interval = Duration.between(
					startAvailability?.toZonedDateTime(
						LocalTime.MIN, now.zone()!!
					), now.toZonedDateTime()
				)

				translateService.getMessage(
					code = "$AVAILABILITY_STATUS_DURATION_PREFIX$model",
					args = arrayOf(formatDuration(interval)),
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
					code = "${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET",
					args = arrayOf(formatDuration(interval)),
				)
			}

			now.asEndIsBeforeOther(endAvailability) -> {
				val interval = Duration.between(
					endAvailability?.toZonedDateTime(
						LocalTime.MAX, now.zone()!!
					), now.toZonedDateTime()
				)

				translateService.getMessage(
					code = "${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE",
					args = arrayOf(formatDuration(interval)),
				)
			}

			else -> translateService.getMessage(code = "$AVAILABILITY_STATUS_PREFIX$model")
		}
	}
}
