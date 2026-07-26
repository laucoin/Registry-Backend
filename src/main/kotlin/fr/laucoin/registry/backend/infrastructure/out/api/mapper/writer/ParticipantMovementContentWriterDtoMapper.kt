package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class ParticipantMovementContentWriterDtoMapper :
	IGenericWriterDtoMapper<MovementContentModel, ParticipantMovementContentWriterDto> {
	override fun toModel(dto: ParticipantMovementContentWriterDto): MovementContentModel {
		return MovementContentModel().apply {
			poolName = dto.poolName
			participant = ParticipantModel().apply { id = dto.participantId }
			vehicle = Optional.ofNullable(dto.vehicleId).map { VehicleModel().apply { id = it } }.orElse(null)
		}
	}
}
