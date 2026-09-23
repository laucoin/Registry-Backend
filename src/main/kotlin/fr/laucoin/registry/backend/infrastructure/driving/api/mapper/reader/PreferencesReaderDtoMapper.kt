package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferencesReaderDto
import org.springframework.stereotype.Component

@Component
class PreferencesReaderDtoMapper: IGenericReaderDtoMapper<PreferencesModel, PreferencesReaderDto> {
	override fun toDto(model: PreferencesModel): PreferencesReaderDto {
		return PreferencesReaderDto(
			theme = model.theme,
			language = model.language,
		).apply {
			id = model.id
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
