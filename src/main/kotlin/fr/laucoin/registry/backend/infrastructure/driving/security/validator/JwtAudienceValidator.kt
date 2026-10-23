package fr.laucoin.registry.backend.infrastructure.driving.security.validator

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Purpose: Validates the audience of the access tokens.
 * Scope: Accepts a token whose audience contains a trusted client id.
 * Limits: Does not check signature or expiry.
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
