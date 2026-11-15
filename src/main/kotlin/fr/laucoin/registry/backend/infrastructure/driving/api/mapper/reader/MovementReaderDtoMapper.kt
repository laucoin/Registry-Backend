package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.MovementReasonMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [MovementTypeReaderDtoMapper::class, MovementReasonMappingHelper::class, MovementContentReaderDtoMapper::class])
interface MovementReaderDtoMapper: IGenericReaderDtoMapper<MovementModel, MovementReaderDto> {
	@Mapping(target = "reason", source = "model", qualifiedByName = ["movementReason"])
	override fun toDto(model: MovementModel): MovementReaderDto
}
