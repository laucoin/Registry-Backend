package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_REASON_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReasonsReaderDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class MovementReasonReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<MovementReasonEnum, MovementReasonsReaderDto> {
    override fun toDto(model: MovementReasonEnum, locale: Locale): MovementReasonsReaderDto {
        return MovementReasonsReaderDto(
            label = translateService.getMessage("$MOVEMENT_REASON_PREFIX$model", null, locale),
            value = model.name,
            type = model.type,
            kind = MovementReasonKindEnum.REASON,
        )
    }
}
