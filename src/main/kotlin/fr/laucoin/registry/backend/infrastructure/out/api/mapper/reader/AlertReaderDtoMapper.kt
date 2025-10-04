package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class AlertReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val statusMapper: AlertStatusReaderDtoMapper,
): IGenericReaderDtoMapper<AlertModel, AlertReaderDto> {
	override fun toDto(model: AlertModel): AlertReaderDto {
		return AlertReaderDto(
			title = model.title,
			dateTime = model.dateTime,
			status = Optional.ofNullable(model.status).map(statusMapper::toDto).orElse(null),
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map(projectMapper::toDto).orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
