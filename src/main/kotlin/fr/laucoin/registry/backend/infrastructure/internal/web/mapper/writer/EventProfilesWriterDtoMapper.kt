package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfilesWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class EventProfilesWriterDtoMapper(private val customDateTimeMapper: CustomDateTimeWriterDtoMapper) {
    fun toModels(dto: EventProfilesWriterDto, eventId: UUID): List<EventProfileModel> {
        return dto.userIds !!.map {
            EventProfileModel().apply {
                role = dto.role
                startAccess = if (Objects.nonNull(dto.startAccess)) customDateTimeMapper.toModel(dto.startAccess !!) else null
                endAccess = if (Objects.nonNull(dto.endAccess)) customDateTimeMapper.toModel(dto.endAccess !!) else null
                event = EventModel().apply { id = eventId }
                user = UserModel().apply { id = it }
            }
        }.toList()
    }
}
