package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.MovementWriterDto.MovementContentWriterDto
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class MovementContentWriterDtoMapper: IGenericWriterDtoMapper<MovementContentModel, MovementContentWriterDto> {
    override fun toModel(dto: MovementContentWriterDto): MovementContentModel {
        return MovementContentModel().apply {
            poolName = dto.poolName
            participant = ParticipantModel().apply { id = dto.participantId }
            vehicle = if (Objects.nonNull(dto.vehicleId)) VehicleModel().apply { id = dto.vehicleId } else null
        }
    }
}
