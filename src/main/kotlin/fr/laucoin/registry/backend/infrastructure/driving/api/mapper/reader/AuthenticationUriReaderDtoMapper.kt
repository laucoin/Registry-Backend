package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AuthenticationUriReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface AuthenticationUriReaderDtoMapper: IGenericReaderDtoMapper<AuthenticationUriModel, AuthenticationUriReaderDto> {
	@Mapping(target = "copy", ignore = true)
	override fun toDto(model: AuthenticationUriModel): AuthenticationUriReaderDto
}
