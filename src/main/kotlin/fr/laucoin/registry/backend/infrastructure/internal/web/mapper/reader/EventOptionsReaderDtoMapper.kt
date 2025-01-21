package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventOptionsReaderDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class EventOptionsReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<Pair<EventOptionEnum, Collection<EventOptionEnum>>, EventOptionsReaderDto> {
    override fun toDto(model: Pair<EventOptionEnum, Collection<EventOptionEnum>>, locale: Locale): EventOptionsReaderDto {
        return EventOptionsReaderDto(
            value = model.first,
            label = translateService.getMessage("$EVENT_OPTION_NAME_PREFIX${model.first}", null, locale),
            ask = translateService.getMessage("$EVENT_OPTION_FORM_ASK_PREFIX${model.first}", null, locale),
            preRequired = model.second.map {
                LabelDto(
                    it.name,
                    translateService.getMessage("$EVENT_OPTION_NAME_PREFIX$it", null, locale)
                )
            }
        )
    }
}
