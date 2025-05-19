package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.ParticipantMovementWriterDto.ParticipantMovementContentWriterDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ParticipantMovementContentWriterDtoMapper: IGenericWriterDtoMapper<MovementContentModel, ParticipantMovementContentWriterDto> {
    override fun toModel(dto: ParticipantMovementContentWriterDto): MovementContentModel {
        return MovementContentModel().apply {
            poolName = dto.poolName
            participant = ParticipantModel().apply { id = dto.participantId }
            vehicle = Optional.ofNullable(dto.vehicleId).map { VehicleModel().apply { id = it } }.orElse(null)
        }
    }
}
