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
    private val projectMapper: ProjectReaderDtoMapper,
    private val activityReasonReaderDtoMapper: MovementActivityReasonReaderDtoMapper,
    private val reasonReaderDtoMapper: MovementReasonReaderDtoMapper,
    private val movementContentMapper: MovementContentReaderDtoMapper,
): IGenericReaderDtoMapper<MovementModel, MovementReaderDto> {
    override fun toDto(model: MovementModel, locale: Locale): MovementReaderDto {
        return MovementReaderDto(
            dateTime = model.dateTime,
            type = if (Objects.nonNull(model.type)) LabelDto(
                model.type !!.name,
                translateService.getMessage("$MOVEMENT_TYPE_PREFIX${model.type}", null, locale),
            ) else null,
            reason = if (Objects.nonNull(model.reason)) reasonReaderDtoMapper.toDto(model.reason !!, locale)
            else if (Objects.nonNull(model.activity)) activityReasonReaderDtoMapper.toDto(model.activity !!, locale)
            else null,
            contentType = model.contentType,
            content = movementContentMapper.toDtoList(model.content, locale),
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
