package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import java.util.Locale
import java.util.Objects
import java.util.Optional
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
            type = Optional.ofNullable(model.type).map {
                LabelDto(
                    it.name,
                    translateService.getMessage("$MOVEMENT_TYPE_PREFIX$it", null, locale),
                )
            }.orElse(null),
            reason = if (Objects.nonNull(model.reason)) reasonReaderDtoMapper.toDto(model.reason !!, locale)
            else if (Objects.nonNull(model.activity)) activityReasonReaderDtoMapper.toDto(model.activity !!, locale)
            else null,
            contentType = model.contentType,
            content = movementContentMapper.toDtoList(model.content, locale),
        ).apply {
            id = model.id
            project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
