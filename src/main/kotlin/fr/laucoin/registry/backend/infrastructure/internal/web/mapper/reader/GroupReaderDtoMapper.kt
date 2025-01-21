package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import org.springframework.stereotype.Component

@Component
class GroupReaderDtoMapper: IGenericReaderDtoMapper<GroupModel, GroupReaderDto> {
    override fun toDto(model: GroupModel): GroupReaderDto {
        return GroupReaderDto(
            id = model.id,
            event = model.event,
            name = model.name,
            begin = model.begin,
            end = model.end,
            visible = model.visible,
            creation = model.creation,
            lastEdition = model.lastEdition,
        )
    }
}
