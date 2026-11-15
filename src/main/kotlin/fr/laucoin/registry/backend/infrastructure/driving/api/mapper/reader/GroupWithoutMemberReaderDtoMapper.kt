package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.GroupWithoutMemberReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class])
interface GroupWithoutMemberReaderDtoMapper: IGenericReaderDtoMapper<GroupModel, GroupWithoutMemberReaderDto> {
	@Mapping(target = "status", source = "model", qualifiedByName = ["groupStatus"])
	override fun toDto(model: GroupModel): GroupWithoutMemberReaderDto
}
