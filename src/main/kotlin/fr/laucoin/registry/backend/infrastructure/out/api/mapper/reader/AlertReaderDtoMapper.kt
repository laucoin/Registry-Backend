package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AlertReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class AlertReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val statusMapper: AlertStatusReaderDtoMapper,
): IGenericReaderDtoMapper<AlertModel, AlertReaderDto> {
	override fun toDto(model: AlertModel, locale: Locale): AlertReaderDto {
		return AlertReaderDto(
			title = model.title,
			dateTime = model.dateTime,
			status = Optional.ofNullable(model.status).map { statusMapper.toDto(it, locale) }.orElse(null),
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
