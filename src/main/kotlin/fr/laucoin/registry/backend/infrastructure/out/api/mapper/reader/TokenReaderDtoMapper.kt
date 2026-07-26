package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.TokenReaderDto
import org.springframework.stereotype.Component

@Component
class TokenReaderDtoMapper : IGenericReaderDtoMapper<TokenModel, TokenReaderDto> {
	override fun toDto(model: TokenModel): TokenReaderDto {
		return TokenReaderDto(
			accessToken = model.accessToken,
			expiresIn = model.expiresIn,
			refreshExpiresIn = model.refreshExpiresIn,
			refreshToken = model.refreshToken,
			tokenType = model.tokenType,
		)
	}
}
