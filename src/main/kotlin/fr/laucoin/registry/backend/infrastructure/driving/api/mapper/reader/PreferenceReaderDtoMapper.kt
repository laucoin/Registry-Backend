package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferenceReaderDto
import org.springframework.stereotype.Component

@Component
class PreferenceReaderDtoMapper: IGenericReaderDtoMapper<PreferencesModel, PreferenceReaderDto> {
	override fun toDto(model: PreferencesModel): PreferenceReaderDto {
		return PreferenceReaderDto(
			theme = model.theme,
			language = model.language,
		)
	}
}
