package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_REASON_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.MovementReasonKindEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReasonsReaderDto
import java.util.Locale
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class MovementActivityReasonReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
): IGenericReaderDtoMapper<ActivityModel, MovementReasonsReaderDto> {
    override fun toDto(
        model: ActivityModel,
        locale: Locale
    ): MovementReasonsReaderDto {
        return MovementReasonsReaderDto(
            label = "${model.name} (${translateService.getMessage("$MOVEMENT_REASON_PREFIX$ACTIVITY", null, locale)})",
            value = model.id !!.toString(),
            kind = MovementReasonKindEnum.ACTIVITY,
        )
    }
}
