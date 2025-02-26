package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USABLE_ELEMENT_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class UsableElementStatusReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<UsableElementStatusEnum, LabelDto> {
    override fun toDto(model: UsableElementStatusEnum, locale: Locale): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage("$USABLE_ELEMENT_STATUS_PREFIX$model", null, locale),
        )
    }
}
