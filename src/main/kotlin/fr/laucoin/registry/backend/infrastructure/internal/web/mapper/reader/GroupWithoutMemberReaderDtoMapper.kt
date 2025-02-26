package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupWithoutMemberReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class GroupWithoutMemberReaderDtoMapper(
    private val eventMapper: EventReaderDtoMapper,
): IGenericReaderDtoMapper<GroupModel, GroupWithoutMemberReaderDto> {
    override fun toDto(model: GroupModel, locale: Locale): GroupWithoutMemberReaderDto {
        return GroupWithoutMemberReaderDto(
            name = model.name,
            startAvailability = model.startAvailability,
            endAvailability = model.endAvailability,
        ).apply {
            id = model.id
            event = if (Objects.nonNull(model.event)) eventMapper.toDto(model.event !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
