package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PlannedMovementReaderDto
import org.springframework.stereotype.Component

@Component
class PlannedMovementReaderDtoMapper(
	private val movementMapper: MovementReaderDtoMapper,
): IGenericReaderDtoMapper<MovementModel, PlannedMovementReaderDto> {
	override fun toDto(model: MovementModel): PlannedMovementReaderDto {
		val movement = movementMapper.toDto(model)
		return PlannedMovementReaderDto(
			type = movement.type,
			reason = movement.reason,
			contentType = movement.contentType,
			content = movement.content,
		)
	}
}
