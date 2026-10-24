package fr.laucoin.registry.backend.infrastructure.driving.security.validator

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Objectif : Valide l'audience des tokens d'accès.
 * Périmètre : Accepte un token dont l'audience contient un id client de confiance.
 * Limites : Ne vérifie ni la signature ni l'expiration.
 */
class JwtAudienceValidator(private val trustedAudiences: Set<String>): OAuth2TokenValidator<Jwt> {
	override fun validate(token: Jwt): OAuth2TokenValidatorResult {
		if (token.audience.orEmpty().any { it in trustedAudiences }) {
			return OAuth2TokenValidatorResult.success()
		}

		return OAuth2TokenValidatorResult.failure(
			OAuth2Error(INVALID_TOKEN, "The token audience is not trusted by this resource server", null)
		)
	}
}
