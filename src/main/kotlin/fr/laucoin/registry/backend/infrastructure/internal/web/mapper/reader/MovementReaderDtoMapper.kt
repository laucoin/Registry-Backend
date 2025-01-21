package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

@Component
class MovementReaderDtoMapper(
    @Qualifier("messagesSource") private val translateService: MessageSource,
    private val eventMapper: EventReaderDtoMapper,
    private val movementContentMapper: MovementContentReaderDtoMapper,
): IGenericReaderDtoMapper<MovementModel, MovementReaderDto> {
    override fun toDto(model: MovementModel, locale: Locale): MovementReaderDto {
        return MovementReaderDto(
            id = model.id,
            event = if (Objects.nonNull(model.event)) eventMapper.toDto(model.event !!, locale) else null,
            dateTime = model.dateTime,
            type = if (Objects.nonNull(model.type)) LabelDto(
                model.type !!.name,
                translateService.getMessage("$MOVEMENT_TYPE_PREFIX${model.type}", null, locale),
            ) else null,
            content = movementContentMapper.toDtoList(model.content, locale),
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
