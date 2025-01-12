package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.GroupDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class GroupDtoMapper: IGenericEventDtoMapper<GroupModel, GroupDto> {
    override fun toModel(dto: GroupDto, eventId: UUID): GroupModel {
        return GroupModel().apply {
            name = dto.name !!
            begin = dto.begin
            end = dto.end
            members = dto.members !!.map { ParticipantModel().apply { id = it } }
            event = EventModel().apply { id = eventId }
        }
    }
}
