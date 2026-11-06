package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AddedGroupMembersReaderDto
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface AddedGroupMembersReaderDtoMapper {
	@Mapping(target = "members", source = "model.first")
	@Mapping(target = "notAddedMemberIds", source = "model.second")
	fun toDto(model: @JvmSuppressWildcards Pair<List<UUID>, List<UUID>>): AddedGroupMembersReaderDto
}
