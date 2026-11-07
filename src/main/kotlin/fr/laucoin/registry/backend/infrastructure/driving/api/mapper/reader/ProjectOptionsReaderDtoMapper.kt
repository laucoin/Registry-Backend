package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [LabelMappingHelper::class, ProjectOptionLabelReaderDtoMapper::class])
interface ProjectOptionsReaderDtoMapper: IGenericReaderDtoMapper<ProjectOptionEnum, ProjectOptionsReaderDto> {
	@Mapping(target = "value", source = "model")
	@Mapping(target = "label", source = "model", qualifiedByName = ["projectOptionNameLabel"])
	@Mapping(target = "ask", source = "model", qualifiedByName = ["projectOptionAskLabel"])
	@Mapping(target = "preRequired", source = "model.requiredOptions")
	override fun toDto(model: ProjectOptionEnum): ProjectOptionsReaderDto
}
