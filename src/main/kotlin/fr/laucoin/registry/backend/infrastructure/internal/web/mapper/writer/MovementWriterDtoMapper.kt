package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class MovementWriterDtoMapper(
    private val contentMapper: MovementContentWriterDtoMapper
): IGenericEventWriterDtoMapper<MovementModel, MovementWriterDto> {
    override fun toModel(dto: MovementWriterDto, eventId: UUID): MovementModel {
        return MovementModel().apply {
            dateTime = dto.dateTime !!
            type = dto.type
            content = dto.content !!.map { contentMapper.toModel(it) }
            event = EventModel().apply { id = eventId }
        }
    }
}
