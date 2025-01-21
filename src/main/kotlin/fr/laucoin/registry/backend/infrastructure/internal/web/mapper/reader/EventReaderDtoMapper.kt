package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class EventReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<EventModel, EventReaderDto> {
    override fun toDto(model: EventModel, locale: Locale): EventReaderDto {
        return EventReaderDto(
            id = model.id,
            name = model.name,
            begin = model.begin,
            end = model.end,
            options = model.options?.map {
                LabelDto(
                    it.name,
                    translateService.getMessage("$EVENT_OPTION_NAME_PREFIX$it", null, locale)
                )
            },
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
