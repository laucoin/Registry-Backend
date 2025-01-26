package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class GroupReaderDtoMapper(
    private val eventMapper: EventReaderDtoMapper,
    private val participantMapper: ParticipantReaderDtoMapper,
): IGenericReaderDtoMapper<GroupModel, GroupReaderDto> {
    override fun toDto(model: GroupModel, locale: Locale): GroupReaderDto {
        return GroupReaderDto(
            members = participantMapper.toDtoList(model.members, locale),
        ).apply {
            id = model.id
            event = if (Objects.nonNull(model.event)) eventMapper.toDto(model.event !!, locale) else null
            name = model.name
            begin = model.begin
            end = model.end
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
