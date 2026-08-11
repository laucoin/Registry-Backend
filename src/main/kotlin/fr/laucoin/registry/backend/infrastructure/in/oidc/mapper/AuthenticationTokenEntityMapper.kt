package fr.laucoin.registry.backend.infrastructure.`in`.oidc.mapper

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.oidc.entity.OidcTokenEntity
import org.springframework.stereotype.Component

@Component
class AuthenticationTokenEntityMapper : IEntityMapper<TokenModel, OidcTokenEntity> {
	override fun toModel(entity: OidcTokenEntity): TokenModel {
		return TokenModel(
			accessToken = entity.accessToken,
			expiresIn = entity.expiresIn,
			refreshExpiresIn = entity.refreshExpiresIn,
			refreshToken = entity.refreshToken,
			tokenType = entity.tokenType,
		)
	}

	override fun toEntity(model: TokenModel): OidcTokenEntity {
		return OidcTokenEntity(
			accessToken = model.accessToken,
			expiresIn = model.expiresIn,
			refreshExpiresIn = model.refreshExpiresIn,
			refreshToken = model.refreshToken,
			tokenType = model.tokenType,
		)
	}
}
