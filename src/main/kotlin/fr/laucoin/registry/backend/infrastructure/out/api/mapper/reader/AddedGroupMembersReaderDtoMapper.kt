package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AddedGroupMembersReaderDto
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class AddedGroupMembersReaderDtoMapper :
	IGenericReaderDtoMapper<Pair<List<UUID>, List<UUID>>, AddedGroupMembersReaderDto> {
	override fun toDto(model: Pair<List<UUID>, List<UUID>>): AddedGroupMembersReaderDto {
		return AddedGroupMembersReaderDto(
			members = model.first,
			notAddedMemberIds = model.second,
		)
	}
}
