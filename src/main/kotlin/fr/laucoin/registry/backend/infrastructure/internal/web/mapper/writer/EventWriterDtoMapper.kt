package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class EventWriterDtoMapper(
    private val customDateTimeMapper: CustomDateTimeWriterDtoMapper,
): IGenericWriterDtoMapper<EventModel, EventWriterDto> {
    override fun toModel(dto: EventWriterDto): EventModel {
        return EventModel().apply {
            name = dto.name
            begin = if (Objects.nonNull(dto.begin)) customDateTimeMapper.toModel(dto.begin !!) else null
            end = if (Objects.nonNull(dto.end)) customDateTimeMapper.toModel(dto.end !!) else null
            options = dto.options
        }
    }
}
