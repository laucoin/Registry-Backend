package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PreferenceReaderDto
import java.util.Locale
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class PreferenceReaderDtoMapper(
    private val profileMapper: EventProfileReaderDtoMapper,
): IGenericReaderDtoMapper<PreferencesModel, PreferenceReaderDto> {
    override fun toDto(model: PreferencesModel, locale: Locale): PreferenceReaderDto {
        return PreferenceReaderDto(
            selectedProfile = if (Objects.nonNull(model.selectedProfile)) profileMapper.toDto(
                model.selectedProfile !!,
                locale
            ) else null,
        )
    }
}
