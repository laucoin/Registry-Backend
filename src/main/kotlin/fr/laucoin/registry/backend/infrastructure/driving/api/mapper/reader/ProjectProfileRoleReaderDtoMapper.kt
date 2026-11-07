package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [LabelMappingHelper::class])
interface ProjectProfileRoleReaderDtoMapper: IGenericReaderDtoMapper<String, LabelDto> {
	@Mapping(target = "value", source = "model")
	@Mapping(target = "label", source = "model", qualifiedByName = ["projectProfileRoleLabel"])
	override fun toDto(model: String): LabelDto
}
