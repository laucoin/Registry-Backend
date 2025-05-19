package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.DURATION_PREFIX
import java.time.Duration
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class GenericDurationReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
) {
    protected fun formatDuration(duration: Duration, locale: Locale): String {
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
