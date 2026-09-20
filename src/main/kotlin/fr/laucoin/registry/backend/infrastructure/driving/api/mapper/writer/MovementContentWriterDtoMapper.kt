package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.MovementContentWriterDto
import org.springframework.stereotype.Component

@Component
class MovementContentWriterDtoMapper: IGenericWriterDtoMapper<MovementContentModel, MovementContentWriterDto> {
	override fun toModel(dto: MovementContentWriterDto): MovementContentModel {
		return MovementContentModel().apply {
			participant = ParticipantModel().apply { id = dto.participantId }
		}
	}
}
