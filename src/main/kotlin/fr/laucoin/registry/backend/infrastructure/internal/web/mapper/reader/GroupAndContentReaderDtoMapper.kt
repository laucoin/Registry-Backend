package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupAndContentReaderDto
import org.springframework.stereotype.Component

@Component
class GroupAndContentReaderDtoMapper(
    private val participantMapper: ParticipantReaderDtoMapper
): IGenericReaderDtoMapper<GroupModel, GroupAndContentReaderDto> {
    override fun toDto(model: GroupModel): GroupAndContentReaderDto {
        return GroupAndContentReaderDto(
            members = participantMapper.toDtoList(model.members),
        ).apply {
            id = model.id
            event = model.event
            name = model.name
            begin = model.begin
            end = model.end
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
