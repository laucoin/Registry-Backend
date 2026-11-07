package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [MovementTypeReaderDtoMapper::class, MovementReasonReaderDtoMapper::class, ParticipantReaderDtoMapper::class])
interface PlannedMovementReaderDtoMapper: IGenericReaderDtoMapper<PlannedMovementModel, PlannedMovementReaderDto> {
	override fun toDto(model: PlannedMovementModel): PlannedMovementReaderDto
}
