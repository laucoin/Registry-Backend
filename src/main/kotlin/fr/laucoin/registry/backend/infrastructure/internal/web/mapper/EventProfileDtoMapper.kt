package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfileDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class EventProfileDtoMapper: IGenericEventDtoMapper<EventProfileModel, EventProfileDto> {
    override fun toModel(dto: EventProfileDto, eventId: UUID): EventProfileModel {
        return EventProfileModel().apply {
            role = dto.role
            startAccess = dto.startAccess
            endAccess = dto.endAccess
            event = EventModel().apply { id = eventId }
        }
    }
}
