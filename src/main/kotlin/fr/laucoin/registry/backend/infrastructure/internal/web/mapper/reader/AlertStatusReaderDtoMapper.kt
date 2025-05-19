package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.ALERT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class AlertStatusReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<AlertStatusEnum, LabelDto> {
    override fun toDto(model: AlertStatusEnum, locale: Locale): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage("$ALERT_STATUS_PREFIX$model", null, locale),
        )
    }
}
