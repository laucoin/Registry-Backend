package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.MovementReaderDto.MovementContentReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class MovementContentReaderDtoMapper(
    private val participantMapper: ParticipantReaderDtoMapper,
    private val vehicleMapper: VehicleReaderDtoMapper,
): IGenericReaderDtoMapper<MovementContentModel, MovementContentReaderDto> {
    override fun toDto(model: MovementContentModel, locale: Locale): MovementContentReaderDto {
        return MovementContentReaderDto(
            poolName = model.poolName,
            participant = if (Objects.nonNull(model.participant)) participantMapper.toDto(model.participant !!, locale) else null,
            vehicle = if (Objects.nonNull(model.vehicle)) vehicleMapper.toDto(model.vehicle !!, locale) else null,
        )
    }
}
