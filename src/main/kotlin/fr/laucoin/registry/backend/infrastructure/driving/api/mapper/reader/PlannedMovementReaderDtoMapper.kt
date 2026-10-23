package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PlannedMovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import org.springframework.stereotype.Component

@Component
class PlannedMovementReaderDtoMapper(
	private val typeMapper: MovementTypeReaderDtoMapper,
	private val reasonMapper: MovementReasonReaderDtoMapper,
	private val participantMapper: ParticipantReaderDtoMapper,
): IGenericReaderDtoMapper<PlannedMovementModel, PlannedMovementReaderDto> {
	override fun toDto(model: PlannedMovementModel): PlannedMovementReaderDto {
		return PlannedMovementReaderDto(
			type = typeMapper.toDto(model.type),
			reason = model.reason?.let(reasonMapper::toDto),
			contentType = model.contentType,
			plannedAt = model.plannedAt,
			poolName = model.poolName,
			participants = participantMapper.toDtoList(model.participants),
		)
	}
}
