package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementReaderDto.MovementContentReaderDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class MovementContentReaderDtoMapper(
	private val participantMapper: ParticipantReaderDtoMapper,
	private val vehicleMapper: VehicleReaderDtoMapper,
) : IGenericReaderDtoMapper<MovementContentModel, MovementContentReaderDto> {
	override fun toDto(model: MovementContentModel): MovementContentReaderDto {
		return MovementContentReaderDto(
			poolName = model.poolName,
			participant = Optional.ofNullable(model.participant).map(participantMapper::toDto).orElse(null),
			vehicle = Optional.ofNullable(model.vehicle).map(vehicleMapper::toDto).orElse(null),
		)
	}
}
