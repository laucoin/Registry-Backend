package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.FeaturesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface FeaturesReaderDtoMapper: IGenericReaderDtoMapper<Boolean?, FeaturesReaderDto> {
	@Mapping(target = "inviteByEmail", source = "model")
	@Mapping(target = "copy", ignore = true)
	override fun toDto(model: Boolean?): FeaturesReaderDto
}
