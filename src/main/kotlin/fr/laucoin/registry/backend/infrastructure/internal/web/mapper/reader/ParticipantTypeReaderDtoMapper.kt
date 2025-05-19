package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PARTICIPANT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class ParticipantTypeReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<ParticipantTypeEnum, LabelDto> {
    override fun toDto(model: ParticipantTypeEnum, locale: Locale): LabelDto {
        return LabelDto(
            model.name,
            translateService.getMessage("$PARTICIPANT_TYPE_PREFIX$model", null, locale),
        )
    }
}
