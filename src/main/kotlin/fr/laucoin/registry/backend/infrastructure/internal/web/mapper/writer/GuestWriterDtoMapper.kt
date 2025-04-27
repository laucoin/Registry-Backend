package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GuestMovementWriterDto.GuestWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GuestWriterDtoMapper: IGenericEventWriterDtoMapper<ParticipantModel, GuestWriterDto> {
    override fun toModel(dto: GuestWriterDto, eventId: UUID): ParticipantModel {
        return ParticipantModel().apply {
            id = dto.id
            firstName = dto.firstName
            lastName = dto.lastName
            birthday = dto.birthday
            type = GUEST
            event = EventModel().apply { id = eventId }
        }
    }
}
