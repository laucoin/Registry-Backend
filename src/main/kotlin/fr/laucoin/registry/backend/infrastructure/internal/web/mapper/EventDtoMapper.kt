package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventDto
import org.springframework.stereotype.Component

@Component
class EventDtoMapper: IGenericDtoMapper<EventModel, EventDto> {
    override fun toModel(dto: EventDto): EventModel {
        return EventModel().apply {
            name = dto.name
            begin = dto.begin
            end = dto.end
            options = dto.options
        }
    }
}
