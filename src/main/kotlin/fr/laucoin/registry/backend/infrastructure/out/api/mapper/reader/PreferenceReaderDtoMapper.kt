package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PreferenceReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class PreferenceReaderDtoMapper(
	private val profileMapper: ProjectProfileReaderDtoMapper,
): IGenericReaderDtoMapper<PreferencesModel, PreferenceReaderDto> {
	override fun toDto(model: PreferencesModel, locale: Locale): PreferenceReaderDto {
		return PreferenceReaderDto(
			theme = model.theme,
			language = model.language,
			selectedProfile = Optional.ofNullable(model.selectedProfile).map {
				profileMapper.toDto(
					model.selectedProfile!!,
					locale
				)
			}.orElse(null),
		)
	}
}
