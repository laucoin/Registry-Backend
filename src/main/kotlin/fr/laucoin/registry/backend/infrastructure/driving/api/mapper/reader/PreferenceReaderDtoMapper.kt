package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.PreferenceReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface PreferenceReaderDtoMapper: IGenericReaderDtoMapper<PreferencesModel, PreferenceReaderDto> {
	override fun toDto(model: PreferencesModel): PreferenceReaderDto
}
