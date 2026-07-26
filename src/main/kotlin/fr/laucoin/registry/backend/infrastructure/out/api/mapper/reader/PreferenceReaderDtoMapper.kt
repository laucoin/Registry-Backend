package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PreferenceReaderDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class PreferenceReaderDtoMapper(
	private val profileMapper: ProjectProfileReaderDtoMapper,
) : IGenericReaderDtoMapper<PreferencesModel, PreferenceReaderDto> {
	override fun toDto(model: PreferencesModel): PreferenceReaderDto {
		return PreferenceReaderDto(
			theme = model.theme,
			language = model.language,
			selectedProfile = Optional.ofNullable(model.selectedProfile)
				.map { profileMapper.toDto(model.selectedProfile!!) }
				.orElse(null),
		)
	}
}
