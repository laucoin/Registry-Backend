package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.CreatedProjectProfilesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface CreatedProjectProfilesReaderDtoMapper {
	@Mapping(target = "createdEmails", source = "model.first")
	@Mapping(target = "notCreatedEmails", source = "model.second")
	fun toDto(model: @JvmSuppressWildcards Pair<List<String>, List<String>>): CreatedProjectProfilesReaderDto
}
