package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.OngoingActivityOutingModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OngoingActivityOutingReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class OngoingActivityOutingReaderDtoMapper(
	private val movementMapper: MovementReaderDtoMapper,
	private val communicationMapper: CommunicationReaderDtoMapper,
) {
	fun toDto(model: OngoingActivityOutingModel): OngoingActivityOutingReaderDto {
		return OngoingActivityOutingReaderDto(
			movement = Optional.ofNullable(model.movement).map(movementMapper::toDto).orElse(null),
			recentCommunications = model.recentCommunications.map(communicationMapper::toDto),
		)
	}
}
