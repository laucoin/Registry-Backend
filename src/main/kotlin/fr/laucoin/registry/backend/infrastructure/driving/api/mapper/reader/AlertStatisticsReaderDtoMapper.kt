package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertStatisticsReaderDto
import org.springframework.stereotype.Component

@Component
class AlertStatisticsReaderDtoMapper: IGenericReaderDtoMapper<AlertStatisticsModel, AlertStatisticsReaderDto> {
	override fun toDto(model: AlertStatisticsModel): AlertStatisticsReaderDto {
		return AlertStatisticsReaderDto(
			resolvedCount = model.resolvedCount,
			canceledCount = model.canceledCount,
			averageResolutionSeconds = model.averageResolutionSeconds,
			realAlertRate = model.realAlertRate,
		)
	}
}
