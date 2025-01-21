package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfileWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class EventProfileWriterDtoMapper: IGenericEventWriterDtoMapper<EventProfileModel, EventProfileWriterDto> {
    override fun toModel(dto: EventProfileWriterDto, eventId: UUID): EventProfileModel {
        return EventProfileModel().apply {
            role = dto.role
            startAccess = dto.startAccess
            endAccess = dto.endAccess
            event = EventModel().apply { id = eventId }
        }
    }
}
