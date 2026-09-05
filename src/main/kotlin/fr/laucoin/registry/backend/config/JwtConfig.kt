package fr.laucoin.registry.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes.INVALID_TOKEN
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

/**
 * Builds the JWT decoder used by the resource server.
 *
 * Configuring only `jwk-set-uri` leaves Spring Boot with [JwtValidators.createDefault], which checks
 * the signature, the token type and the timestamps — but neither the issuer nor the audience. Every
 * client of the same provider realm is signed by the same keys and published at the same JWKS
 * endpoint, so a token minted for another application would verify cleanly here and be converted
 * into a full application principal.
 *
 * The decoder is deliberately built from the JWKS URI rather than from `issuer-uri`: the latter makes
 * Spring fetch the provider metadata when the bean is created, which would require a reachable
 * identity provider at every application — and test — startup. Validating the `iss` claim gives the
 * same guarantee without that coupling.
 */
@Configuration
class JwtConfig(
	@param:Value($$"${registry.security.oauth2.jwks-uri}")
	private val jwksUri: String,
	@param:Value($$"${registry.security.oauth2.issuer}")
	private val issuer: String,
	@param:Value($$"${registry.security.oauth2.audience}")
	private val audience: String,
) {

	@Bean
	fun jwtValidator(): OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
		JwtValidators.createDefaultWithIssuer(issuer),
		audienceValidator(),
	)

	/**
	 * Rejects a token whose `aud` claim does not name this application. The claim is absent on some
	 * providers, so it is treated as nullable rather than trusted to be present.
	 */
	private fun audienceValidator() = OAuth2TokenValidator<Jwt> { jwt ->
		if (jwt.audience?.contains(audience) == true) OAuth2TokenValidatorResult.success()
		else OAuth2TokenValidatorResult.failure(
			OAuth2Error(INVALID_TOKEN, "The required audience \"$audience\" is missing", null)
		)
	}

	@Bean
	fun jwtDecoder(jwtValidator: OAuth2TokenValidator<Jwt>): ReactiveJwtDecoder {
		return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build().apply {
			setJwtValidator(jwtValidator)
		}
	}
}
