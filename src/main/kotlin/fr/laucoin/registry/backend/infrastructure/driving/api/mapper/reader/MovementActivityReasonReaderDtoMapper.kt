package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [LabelMappingHelper::class])
interface MovementActivityReasonReaderDtoMapper: IGenericReaderDtoMapper<ActivityModel, MovementReasonsReaderDto> {
	@Mapping(target = "label", source = "model", qualifiedByName = ["activityReasonLabel"])
	@Mapping(target = "value", expression = "java(model.getId().toString())")
	@Mapping(target = "kind", constant = "ACTIVITY")
	@Mapping(target = "type", ignore = true)
	override fun toDto(model: ActivityModel): MovementReasonsReaderDto
}
