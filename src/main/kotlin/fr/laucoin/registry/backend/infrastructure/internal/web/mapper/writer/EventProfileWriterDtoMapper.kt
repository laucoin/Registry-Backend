package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventProfileWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class EventProfileWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericEventWriterDtoMapper<EventProfileModel, EventProfileWriterDto> {
    override fun toModel(dto: EventProfileWriterDto, eventId: UUID): EventProfileModel {
        return EventProfileModel().apply {
            role = dto.role
            startAccess = if (Objects.nonNull(dto.startAccess)) customDateTimeMapper.toModel(dto.startAccess !!) else null
            endAccess = if (Objects.nonNull(dto.endAccess)) customDateTimeMapper.toModel(dto.endAccess !!) else null
            event = EventModel().apply { id = eventId }
        }
    }
}
