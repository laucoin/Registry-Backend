package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_DURATION_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PRESENCE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.OUT
import fr.laucoin.registry.backend.domain.extension.DateExt.isAfter
import fr.laucoin.registry.backend.domain.extension.DateExt.isBefore
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.time.Duration
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class PresenceStatusReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): GenericDurationReaderDtoMapper(translateService) {
    fun toDto(
        model: PresenceStatusEnum,
        locale: Locale,
    ): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage("$PRESENCE_STATUS_PREFIX$model", null, locale),
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
                    "$PRESENCE_STATUS_DURATION_PREFIX$model",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            Objects.nonNull(startAvailability) && startAvailability.isAfter(now) -> {
                val interval = Duration.between(
                    now.toZonedDateTime(),
                    startAvailability?.toZonedDateTime(
                        OffsetTime.MIN
                    )
                )

                translateService.getMessage(
                    "${PRESENCE_STATUS_DURATION_PREFIX}ARRIVE",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            Objects.nonNull(endAvailability) && endAvailability.isBefore(now) -> {
                val interval = Duration.between(
                    endAvailability?.toZonedDateTime(
                        OffsetTime.MAX
                    ), now.toZonedDateTime()
                )

                translateService.getMessage(
                    "${PRESENCE_STATUS_DURATION_PREFIX}LEFT",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            Objects.isNull(lastMovement) -> {
                translateService.getMessage(
                    "${PRESENCE_STATUS_PREFIX}NOT_ARRIVED_YET",
                    null,
                    locale
                )
            }

            else -> translateService.getMessage("$PRESENCE_STATUS_PREFIX$model", null, locale)
        }
    }
}
