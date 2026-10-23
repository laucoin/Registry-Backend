package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertLastCommunicationReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class AlertReaderDtoMapper(
	private val statusMapper: AlertStatusReaderDtoMapper,
	private val criticalityMapper: AlertCriticalityReaderDtoMapper,
): IGenericReaderDtoMapper<AlertModel, AlertReaderDto> {
	override fun toDto(model: AlertModel): AlertReaderDto {
		return AlertReaderDto(
			title = model.title,
			dateTime = model.dateTime,
			status = Optional.ofNullable(model.status).map(statusMapper::toDto).orElse(null),
			criticality = Optional.ofNullable(model.criticality).map(criticalityMapper::toDto).orElse(null),
			lastCommunication = model.lastCommunication?.let { AlertLastCommunicationReaderDto(it.dateTime, it.message) },
		).apply {
			id = model.id
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
