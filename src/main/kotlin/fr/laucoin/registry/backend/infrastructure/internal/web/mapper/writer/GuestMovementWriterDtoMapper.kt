package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GuestMovementWriterDto
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GuestMovementWriterDtoMapper(
    private val contentMapper: MovementContentWriterDtoMapper
): IGenericEventWriterDtoMapper<MovementModel, GuestMovementWriterDto> {
    override fun toModel(dto: GuestMovementWriterDto, eventId: UUID): MovementModel {
        return MovementModel(contentType = GUEST).apply {
            dateTime = dto.dateTime !!
            type = dto.type
            reason = dto.reason
            content = Optional.ofNullable(dto.content).map(contentMapper::toModels).orElse(emptyList())
            event = EventModel().apply { id = eventId }
        }
    }
}
