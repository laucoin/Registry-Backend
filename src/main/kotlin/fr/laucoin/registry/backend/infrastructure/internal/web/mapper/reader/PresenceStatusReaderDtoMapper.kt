package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
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
) {
    fun toDto(
        model: PresenceStatusEnum,
        locale: Locale,
        lastMovement: ZonedDateTime? = null,
        startAvailability: CustomDateTimeModel? = null,
        endAvailability: CustomDateTimeModel? = null,
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

                return translateService.getMessage(
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

                return translateService.getMessage(
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

                return translateService.getMessage(
                    "${PRESENCE_STATUS_DURATION_PREFIX}LEFT",
                    arrayOf(formatDuration(interval, locale)),
                    locale
                )
            }

            Objects.nonNull(lastMovement) && OUT == model -> {
                return translateService.getMessage(
                    "${PRESENCE_STATUS_DURATION_PREFIX}NOT_ARRIVED_YET",
                    null,
                    locale
                )
            }

            else -> translateService.getMessage("$PRESENCE_STATUS_PREFIX$model", null, locale)
        }
    }

    private fun formatDuration(duration: Duration, locale: Locale): String {
        return when {
            duration.seconds == 1L -> translateService.getMessage("${DURATION_PREFIX}second", null, locale)
            duration.seconds < 60 -> translateService.getMessage("${DURATION_PREFIX}seconds", arrayOf(duration.seconds), locale)
            duration.toMinutes() == 1L -> translateService.getMessage("${DURATION_PREFIX}minute", null, locale)
            duration.toMinutes() <= 60 -> translateService.getMessage(
                "${DURATION_PREFIX}minutes",
                arrayOf(duration.toMinutes()),
                locale
            )

            duration.toHours() == 1L -> translateService.getMessage("${DURATION_PREFIX}hour", null, locale)
            duration.toHours() < 24 -> translateService.getMessage("${DURATION_PREFIX}hours", arrayOf(duration.toHours()), locale)
            duration.toDays() == 1L -> translateService.getMessage("${DURATION_PREFIX}day", null, locale)
            duration.toDays() < 31 -> translateService.getMessage("${DURATION_PREFIX}days", arrayOf(duration.toDays()), locale)
            duration.toDays() < 61 -> translateService.getMessage("${DURATION_PREFIX}month", null, locale)
            duration.toDays() < 365 -> translateService.getMessage(
                "${DURATION_PREFIX}months",
                arrayOf((duration.toDays() / 30L).toInt()),
                locale
            )

            duration.toDays() < 730 -> translateService.getMessage("${DURATION_PREFIX}year", null, locale)
            else -> translateService.getMessage("${DURATION_PREFIX}years", arrayOf((duration.toDays() / 365L).toInt()), locale)
        }
    }
}
