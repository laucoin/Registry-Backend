package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [LabelMappingHelper::class])
interface ProjectProfileStatusReaderDtoMapper: IGenericReaderDtoMapper<ProfileStatusEnum, LabelDto> {
	@Mapping(target = "value", expression = "java(model.name())")
	@Mapping(target = "label", source = "model", qualifiedByName = ["profileStatusLabel"])
	override fun toDto(model: ProfileStatusEnum): LabelDto
}
