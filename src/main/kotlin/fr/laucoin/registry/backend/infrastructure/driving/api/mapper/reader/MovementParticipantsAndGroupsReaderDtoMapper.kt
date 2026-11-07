package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [ParticipantReaderDtoMapper::class, GroupReaderDtoMapper::class])
interface MovementParticipantsAndGroupsReaderDtoMapper {
	@Mapping(target = "participants", source = "model.first")
	@Mapping(target = "groups", source = "model.second")
	fun toDto(model: @JvmSuppressWildcards Pair<List<ParticipantModel>, List<GroupModel>>): MovementParticipantsAndGroupsReaderDto
}
