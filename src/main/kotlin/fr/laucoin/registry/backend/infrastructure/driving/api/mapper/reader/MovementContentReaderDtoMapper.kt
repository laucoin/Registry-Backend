package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [ParticipantReaderDtoMapper::class, VehicleReaderDtoMapper::class])
interface MovementContentReaderDtoMapper: IGenericReaderDtoMapper<MovementContentModel, MovementContentReaderDto> {
	override fun toDto(model: MovementContentModel): MovementContentReaderDto
}
