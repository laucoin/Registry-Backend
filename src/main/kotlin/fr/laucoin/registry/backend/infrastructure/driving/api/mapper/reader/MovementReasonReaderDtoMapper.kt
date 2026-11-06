package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReasonsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [LabelMappingHelper::class])
interface MovementReasonReaderDtoMapper: IGenericReaderDtoMapper<MovementReasonEnum, MovementReasonsReaderDto> {
	@Mapping(target = "label", source = "model", qualifiedByName = ["movementReasonLabel"])
	@Mapping(target = "value", expression = "java(model.name())")
	@Mapping(target = "type", source = "model.type")
	@Mapping(target = "kind", constant = "REASON")
	override fun toDto(model: MovementReasonEnum): MovementReasonsReaderDto
}
