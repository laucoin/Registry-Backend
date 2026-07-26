package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PreferencesReaderDto
import org.springframework.stereotype.Component

@Component
class PreferencesReaderDtoMapper : IGenericReaderDtoMapper<PreferencesModel, PreferencesReaderDto> {
	override fun toDto(model: PreferencesModel): PreferencesReaderDto {
		return PreferencesReaderDto(
			id = model.id,
			visible = model.visible,
			creation = model.creation,
			lastEdition = model.lastEdition,
			userId = model.userId,
			theme = model.theme,
			language = model.language,
			selectedProfile = model.selectedProfile,
		)
	}
}
