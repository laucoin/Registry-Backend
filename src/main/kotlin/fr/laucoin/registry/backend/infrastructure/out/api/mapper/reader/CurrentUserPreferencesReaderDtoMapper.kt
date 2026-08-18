package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.CurrentUserPreferencesReaderDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class CurrentUserPreferencesReaderDtoMapper(
	private val profileMapper: ProjectProfileReaderDtoMapper,
) : IGenericReaderDtoMapper<PreferencesModel, CurrentUserPreferencesReaderDto> {
	override fun toDto(model: PreferencesModel): CurrentUserPreferencesReaderDto {
		return CurrentUserPreferencesReaderDto(
			theme = model.theme,
			language = model.language,
			selectedProfile = Optional.ofNullable(model.selectedProfile)
				.map { profileMapper.toDto(model.selectedProfile!!) }
				.orElse(null),
		)
	}
}
