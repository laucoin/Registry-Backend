package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.AVAILABILITY_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isAfter
import fr.laucoin.registry.backend.domain.extension.DateExt.isBefore
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.Duration
import java.time.LocalTime
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class AvailabilityStatusReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): GenericDurationReaderDtoMapper(translateService) {
    fun toDto(
        model: AvailabilityStatusEnum,
        locale: Locale,
    ): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage("$AVAILABILITY_STATUS_PREFIX$model", null, locale),
        )
    }

    fun toDto(
        model: AvailabilityStatusEnum,
        locale: Locale,
        startAvailability: CustomDateTimeModel?,
        endAvailability: CustomDateTimeModel?,
    ): LabelDto {
        return LabelDto(
            model.name,
            extractLabelDuration(model, startAvailability, endAvailability, locale),
        )
    }

    private fun extractLabelDuration(
        model: AvailabilityStatusEnum,
        startAvailability: CustomDateTimeModel?,
        endAvailability: CustomDateTimeModel?,
        locale: Locale
    ): String {
        val now = CustomDateTimeModel.now()
        return when {
            Objects.nonNull(startAvailability) && AvailabilityStatusEnum.AVAILABLE === model -> {
                val interval = Duration.between(
                    startAvailability?.toZonedDateTime(
                        LocalTime.MIN, now.zone() !!
                    ), now.toZonedDateTime()
                )

                translateService.getMessage(
                    "$AVAILABILITY_STATUS_DURATION_PREFIX$model",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            Objects.nonNull(startAvailability) && startAvailability.isAfter(now) -> {
                val interval = Duration.between(
                    now.toZonedDateTime(),
                    startAvailability?.toZonedDateTime(
                        LocalTime.MIN, now.zone() !!
                    )
                )

                translateService.getMessage(
                    "${AVAILABILITY_STATUS_DURATION_PREFIX}NOT_YET",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            Objects.nonNull(endAvailability) && endAvailability.isBefore(now) -> {
                val interval = Duration.between(
                    endAvailability?.toZonedDateTime(
                        LocalTime.MAX, now.zone() !!
                    ), now.toZonedDateTime()
                )

                translateService.getMessage(
                    "${AVAILABILITY_STATUS_DURATION_PREFIX}NO_MORE",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            else -> translateService.getMessage("$AVAILABILITY_STATUS_PREFIX$model", null, locale)
        }
    }
}
