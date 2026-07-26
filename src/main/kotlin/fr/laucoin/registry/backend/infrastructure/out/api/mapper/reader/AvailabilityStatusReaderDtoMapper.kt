package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalTime
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
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): LabelDto {
		return LabelDto(
			model.name,
			extractLabelDuration(model, startAvailability, endAvailability),
		)
	}

	/**
	 * A null availability (or a date-less one) has no duration to compute: the
	 * comparison extensions treat "after/before null" as true, which used to
	 * route null windows into Duration.between(…, null) and crash every
	 * response containing such an entity (e.g. a freshly created project's
	 * creator profile). Null windows fall through to the plain status label
	 * instead.
	 */
	private fun extractLabelDuration(
		model: AvailabilityStatusEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now()
		val start = startAvailability?.toZonedDateTime(LocalTime.MIN, now.zone()!!)
		val end = endAvailability?.toZonedDateTime(LocalTime.MAX, now.zone()!!)
		return when {
			Objects.nonNull(start) && AvailabilityStatusEnum.AVAILABLE === model -> {
				val interval = Duration.between(start, now.toZonedDateTime())

				translateService.getMessage(
					code = "$AVAILABILITY_STATUS_DURATION_PREFIX$model",
					args = arrayOf(formatDuration(interval)),
				)
			}

			Objects.nonNull(start) && now.asStartIsAfterOther(startAvailability) -> {
				val interval = Duration.between(now.toZonedDateTime(), start)

				translateService.getMessage(
					code = "${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET",
					args = arrayOf(formatDuration(interval)),
				)
			}

			Objects.nonNull(end) && now.asEndIsBeforeOther(endAvailability) -> {
				val interval = Duration.between(end, now.toZonedDateTime())

				translateService.getMessage(
					code = "${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE",
					args = arrayOf(formatDuration(interval)),
				)
			}

			else -> translateService.getMessage(code = "$AVAILABILITY_STATUS_PREFIX$model")
		}
	}
}
