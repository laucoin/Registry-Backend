package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto
import org.springframework.stereotype.Component

@Component
class MovementReaderDtoMapper(
    private val movementContentMapper: MovementContentReaderDtoMapper
): IGenericReaderDtoMapper<MovementModel, MovementReaderDto> {
    override fun toDto(model: MovementModel): MovementReaderDto {
        return MovementReaderDto(
            id = model.id,
            event = model.event,
            dateTime = model.dateTime,
            type = model.type,
            content = movementContentMapper.toDtoList(model.content),
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
