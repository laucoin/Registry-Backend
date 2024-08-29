package fr.laucoin.registry.backend.infrastructure.internal.web.mapper

import fr.laucoin.registry.backend.domain.model.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.MovementContentDto
import org.springframework.stereotype.Component

@Component
class MovementContentDtoMapper: IGenericDtoMapper<MovementContentModel, MovementContentDto> {
    override fun toModel(dto: MovementContentDto): MovementContentModel {
        return MovementContentModel().apply {
            participant = ParticipantModel().apply { id = dto.participantId }
        }
    }
}
