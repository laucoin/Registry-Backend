package fr.laucoin.registry.backend.infrastructure.driving.security.validator

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Purpose: Validates the issuer origin of the access tokens.
 * Scope: Accepts a token whose issuer has exactly the trusted scheme, host and port.
 * Limits: Does not check the issuer path, signature or expiry.
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
