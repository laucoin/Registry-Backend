package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.SessionReaderDto
import org.springframework.stereotype.Component

@Component
class SessionReaderDtoMapper: IGenericReaderDtoMapper<TokenModel, SessionReaderDto> {
	override fun toDto(model: TokenModel): SessionReaderDto {
		return SessionReaderDto(
			expiresIn = model.expiresIn,
			refreshExpiresIn = model.refreshExpiresIn,
		)
	}
}
