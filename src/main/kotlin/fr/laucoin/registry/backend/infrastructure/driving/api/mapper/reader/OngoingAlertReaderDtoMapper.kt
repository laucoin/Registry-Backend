package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.OngoingAlertModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OngoingAlertReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class OngoingAlertReaderDtoMapper(
	private val alertMapper: AlertReaderDtoMapper,
	private val communicationMapper: CommunicationReaderDtoMapper,
) {
	fun toDto(model: OngoingAlertModel): OngoingAlertReaderDto {
		return OngoingAlertReaderDto(
			alert = Optional.ofNullable(model.alert).map(alertMapper::toDto).orElse(null),
			recentCommunications = model.recentCommunications.map(communicationMapper::toDto),
		)
	}
}
