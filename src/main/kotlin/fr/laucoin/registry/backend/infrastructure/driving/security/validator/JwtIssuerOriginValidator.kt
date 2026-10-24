package fr.laucoin.registry.backend.infrastructure.driving.security.validator

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Objectif : Valide l'origine de l'émetteur des tokens d'accès.
 * Périmètre : Accepte un token dont l'émetteur a exactement le schéma, l'hôte et le port de confiance.
 * Limites : Ne vérifie ni le chemin de l'émetteur, ni la signature, ni l'expiration.
 */
class JwtIssuerOriginValidator(private val trustedOrigin: String) : OAuth2TokenValidator<Jwt> {
	override fun validate(token: Jwt): OAuth2TokenValidatorResult {
		val issuer = token.issuer
		if (issuer != null && "${issuer.protocol}://${issuer.authority}".equals(trustedOrigin, ignoreCase = true)) {
			return OAuth2TokenValidatorResult.success()
		}

		return OAuth2TokenValidatorResult.failure(
			OAuth2Error(INVALID_TOKEN, "The token issuer is not trusted by this resource server", null)
		)
	}
}
