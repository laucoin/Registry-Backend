package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupWithoutMemberReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class GroupWithoutMemberReaderDtoMapper(
    private val projectMapper: ProjectReaderDtoMapper,
): IGenericReaderDtoMapper<GroupModel, GroupWithoutMemberReaderDto> {
    override fun toDto(model: GroupModel, locale: Locale): GroupWithoutMemberReaderDto {
        return GroupWithoutMemberReaderDto(
            name = model.name,
            startAvailability = model.startAvailability,
            endAvailability = model.endAvailability,
            membersCount = model.membersCount,
            insideMembersCount = model.insideMembersCount,
            outsideMembersCount = model.outsideMembersCount,
        ).apply {
            id = model.id
            project = if (Objects.nonNull(model.project)) projectMapper.toDto(model.project !!, locale) else null
            visible = model.visible
            creation = model.creation
            lastEdition = model.lastEdition
        }
    }
}
