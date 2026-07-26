package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.AuthenticationInfoModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AuthenticationInfoWriterDto
import org.springframework.stereotype.Component

@Component
class AuthenticationInfoWriterDtoMapper :
	IGenericWriterDtoMapper<AuthenticationInfoModel, AuthenticationInfoWriterDto> {
	override fun toModel(dto: AuthenticationInfoWriterDto): AuthenticationInfoModel {
		return AuthenticationInfoModel(
			redirectUri = dto.redirectUri,
			authorizationCode = dto.authorizationCode,
		)
	}
}
