package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import org.springframework.stereotype.Component
import java.time.Duration

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
	 *
	 * Which of the three duration labels applies is decided on the INSTANT line,
	 * not through the availability comparison helpers. Those answer "is now past
	 * the start" / "is now before the end", and both branches below asked for the
	 * opposite of what they meant: "not available yet" fired once the window had
	 * already opened, and "no longer available" fired while it was still open —
	 * which is how a window closing next month reported that it had closed a few
	 * seconds ago. Boundaries come from the model's own asStart/asEnd, so a
	 * date-only window opens at midnight and closes at 23:59:59.
	 */
	private fun extractLabelDuration(
		model: AvailabilityStatusEnum,
		startAvailability: CustomDateTimeModel?,
		endAvailability: CustomDateTimeModel?,
	): String {
		val now = CustomDateTimeModel.now().toZonedDateTime()
		val start = startAvailability?.asStart()
		val end = endAvailability?.asEnd()
		return when {
			start != null && start.isAfter(now) -> translateService.getMessage(
				code = "${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET",
				args = arrayOf(formatDuration(Duration.between(now, start))),
			)

			end != null && end.isBefore(now) -> translateService.getMessage(
				code = "${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE",
				args = arrayOf(formatDuration(Duration.between(end, now))),
			)

			start != null && AvailabilityStatusEnum.AVAILABLE === model -> translateService.getMessage(
				code = "$AVAILABILITY_STATUS_DURATION_PREFIX$model",
				args = arrayOf(formatDuration(Duration.between(start, now))),
			)

			else -> translateService.getMessage(code = "$AVAILABILITY_STATUS_PREFIX$model")
		}
	}
}
