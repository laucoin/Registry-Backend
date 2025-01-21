package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.GroupWriterDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GroupWriterDtoMapper: IGenericEventWriterDtoMapper<GroupModel, GroupWriterDto> {
    override fun toModel(dto: GroupWriterDto, eventId: UUID): GroupModel {
        return GroupModel().apply {
            name = dto.name !!
            begin = dto.begin
            end = dto.end
            members = dto.members !!.map { ParticipantModel().apply { id = it } }
            event = EventModel().apply { id = eventId }
        }
    }
}
