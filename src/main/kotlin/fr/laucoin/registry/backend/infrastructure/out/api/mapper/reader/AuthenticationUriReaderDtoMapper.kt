package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.AuthenticationUriReaderDto
import org.springframework.stereotype.Component

@Component
class AuthenticationUriReaderDtoMapper : IGenericReaderDtoMapper<AuthenticationUriModel, AuthenticationUriReaderDto> {
	override fun toDto(model: AuthenticationUriModel): AuthenticationUriReaderDto {
		return AuthenticationUriReaderDto(
			uri = model.uri,
		)
	}
}
