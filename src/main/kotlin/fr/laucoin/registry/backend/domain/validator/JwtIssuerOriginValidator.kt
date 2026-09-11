package fr.laucoin.registry.backend.domain.validator

import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Authentik issues one `iss` per application slug (e.g. `.../application/o/registry/` vs.
 * `.../application/o/registry-swagger/`), so this only pins the trusted scheme+host+port rather
 * than the full issuer string — it still rejects tokens minted by an unrelated identity provider.
 */
class JwtIssuerOriginValidator(private val trustedOrigin: String): OAuth2TokenValidator<Jwt> {
	override fun validate(token: Jwt): OAuth2TokenValidatorResult {
		val issuer = token.issuer?.toString()
		if (issuer != null && issuer.startsWith(trustedOrigin)) {
			return OAuth2TokenValidatorResult.success()
		}

		return OAuth2TokenValidatorResult.failure(
			OAuth2Error(INVALID_TOKEN, "The token issuer is not trusted by this resource server", null)
		)
	}
}
