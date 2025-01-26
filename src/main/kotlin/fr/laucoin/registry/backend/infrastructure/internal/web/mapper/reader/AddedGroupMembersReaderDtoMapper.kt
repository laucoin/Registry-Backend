package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.AddedGroupMembersReaderDto
import java.util.Locale
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class AddedGroupMembersReaderDtoMapper: IGenericReaderDtoMapper<Pair<List<UUID>, List<UUID>>, AddedGroupMembersReaderDto> {
    override fun toDto(model: Pair<List<UUID>, List<UUID>>, locale: Locale): AddedGroupMembersReaderDto {
        return AddedGroupMembersReaderDto(
            members = model.first,
            notAddedMemberIds = model.second,
        )
    }
}
