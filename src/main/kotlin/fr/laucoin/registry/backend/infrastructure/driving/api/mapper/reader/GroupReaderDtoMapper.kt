package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class, ParticipantReaderDtoMapper::class])
interface GroupReaderDtoMapper: IGenericReaderDtoMapper<GroupModel, GroupReaderDto> {
	@Mapping(target = "status", source = "model", qualifiedByName = ["groupStatus"])
	@Mapping(target = "copy", ignore = true)
	@Mapping(target = "onSiteMembersCount", ignore = true)
	@Mapping(target = "inMembersCount", ignore = true)
	@Mapping(target = "outMembersCount", ignore = true)
	@Mapping(target = "overdueMembersCount", ignore = true)
	override fun toDto(model: GroupModel): GroupReaderDto
}
