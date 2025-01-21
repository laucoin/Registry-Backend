package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
import org.springframework.stereotype.Component

@Component
class EventWriterDtoMapper: IGenericWriterDtoMapper<EventModel, EventWriterDto> {
    override fun toModel(dto: EventWriterDto): EventModel {
        return EventModel().apply {
            name = dto.name
            begin = dto.begin
            end = dto.end
            options = dto.options
        }
    }
}
