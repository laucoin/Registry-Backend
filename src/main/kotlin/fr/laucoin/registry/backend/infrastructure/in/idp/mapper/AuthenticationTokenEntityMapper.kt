package fr.laucoin.registry.backend.infrastructure.`in`.idp.mapper

import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.idp.entity.IdpTokenEntity
import org.springframework.stereotype.Component

@Component
class AuthenticationTokenEntityMapper: IEntityMapper<TokenModel, IdpTokenEntity> {
	override fun toModel(entity: IdpTokenEntity): TokenModel {
		return TokenModel(
			accessToken = entity.accessToken,
			expiresIn = entity.expiresIn,
			refreshToken = entity.refreshToken,
			tokenType = entity.tokenType,
		)
	}

	override fun toEntity(model: TokenModel): IdpTokenEntity {
		return IdpTokenEntity(
			accessToken = model.accessToken,
			expiresIn = model.expiresIn,
			refreshToken = model.refreshToken,
			tokenType = model.tokenType,
		)
	}
}
