package fr.laucoin.registry.backend.infrastructure.`in`.keycloak.mapper

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.keycloak.entity.KeycloakTokenEntity
import org.springframework.stereotype.Component

@Component
class AuthenticationTokenEntityMapper : IEntityMapper<TokenModel, KeycloakTokenEntity> {
	override fun toModel(entity: KeycloakTokenEntity): TokenModel {
		return TokenModel(
			accessToken = entity.accessToken,
			expiresIn = entity.expiresIn,
			refreshExpiresIn = entity.refreshExpiresIn,
			refreshToken = entity.refreshToken,
			tokenType = entity.tokenType,
		)
	}

	override fun toEntity(model: TokenModel): KeycloakTokenEntity {
		return KeycloakTokenEntity(
			accessToken = model.accessToken,
			expiresIn = model.expiresIn,
			refreshExpiresIn = model.refreshExpiresIn,
			refreshToken = model.refreshToken,
			tokenType = model.tokenType,
		)
	}
}
