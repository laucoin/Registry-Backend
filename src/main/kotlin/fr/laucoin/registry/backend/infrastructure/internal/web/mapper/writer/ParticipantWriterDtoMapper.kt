package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantWriterDto
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantWriterDtoMapper: IGenericEventWriterDtoMapper<ParticipantModel, ParticipantWriterDto> {
    override fun toModel(dto: ParticipantWriterDto, eventId: UUID): ParticipantModel {
        return ParticipantModel().apply {
            firstName = dto.firstName
            lastName = dto.lastName
            birthday = dto.birthday
            begin = dto.begin
            end = dto.end
            user = if (Objects.nonNull(dto.userId)) UserModel().apply { id = dto.userId } else null
            groups = if (Objects.nonNull(dto.groupIds)) dto.groupIds !!.map { GroupModel().apply { id = it } } else emptyList()
            event = EventModel().apply { id = eventId }
        }
    }
}
