package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto.MovementContentReaderDto
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class MovementContentReaderDtoMapper(
    private val participantMapper: ParticipantReaderDtoMapper
): IGenericReaderDtoMapper<MovementContentModel, MovementContentReaderDto> {
    override fun toDto(model: MovementContentModel): MovementContentReaderDto {
        return MovementContentReaderDto(
            participant = if (Objects.nonNull(model.participant)) participantMapper.toDto(model.participant !!) else null,
        )
    }
}
