package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventProfilesDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class EventProfilesDtoMapper {
    fun toModels(dto: EventProfilesDto, eventId: UUID): List<EventProfileModel> {
        return dto.userIds !!.map {
            EventProfileModel().apply {
                role = dto.role
                startAccess = dto.startAccess
                endAccess = dto.endAccess
                event = EventModel().apply { id = eventId }
                user = UserModel().apply { id = it }
            }
        }.toList()
    }
}
