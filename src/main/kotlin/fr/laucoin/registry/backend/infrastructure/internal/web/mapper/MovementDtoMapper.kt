package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.MovementDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class MovementDtoMapper(
    private val contentMapper: MovementContentDtoMapper
): IGenericEventDtoMapper<MovementModel, MovementDto> {
    override fun toModel(dto: MovementDto, eventId: UUID): MovementModel {
        return MovementModel().apply {
            dateTime = dto.dateTime !!
            type = dto.type
            content = dto.content !!.map { contentMapper.toModel(it) }
            event = EventModel().apply { id = eventId }
        }
    }
}
