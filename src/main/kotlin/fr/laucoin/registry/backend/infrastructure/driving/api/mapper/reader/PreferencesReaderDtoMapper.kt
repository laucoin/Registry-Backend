package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferencesReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class PreferencesReaderDtoMapper(
	private val projectProfileMapper: ProjectProfileReaderDtoMapper,
): IGenericReaderDtoMapper<PreferencesModel, PreferencesReaderDto> {
	override fun toDto(model: PreferencesModel): PreferencesReaderDto {
		return PreferencesReaderDto(
			theme = model.theme,
			language = model.language,
			selectedProfile = Optional.ofNullable(model.selectedProfile).map(projectProfileMapper::toDto).orElse(null),
		).apply {
			id = model.id
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
