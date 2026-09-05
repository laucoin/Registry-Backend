package fr.laucoin.registry.backend.config

import org.slf4j.LoggerFactory
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
	@param:Value($$"${registry.security.oauth2.audiences}")
	private val audiences: List<String>,
) {
	private val log = LoggerFactory.getLogger(this::class.java)

	@Bean
	fun jwtValidator(): OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(
		JwtValidators.createDefaultWithIssuer(issuer),
		audienceValidator(),
	)

	/**
	 * Rejects a token whose `aud` claim names none of this deployment's clients.
	 *
	 * Several clients legitimately reach the same API — the frontend goes through the backend's
	 * confidential client, while Swagger UI authenticates as its own — so the claim is matched
	 * against a list rather than a single value. The claim is absent on some providers, so it is
	 * treated as nullable rather than trusted to be present.
	 *
	 * A rejection names the audience the token actually carried. Without that, a provider whose
	 * `aud` does not match the configuration produces nothing but silent 401s, and the operator has
	 * no way to discover which value to configure.
	 */
	private fun audienceValidator() = OAuth2TokenValidator<Jwt> { jwt ->
		val presented = jwt.audience.orEmpty()
		if (presented.any(audiences::contains)) OAuth2TokenValidatorResult.success()
		else {
			log.warn(
				"Rejecting a token issued for audience {}: none of the accepted audiences {} is present. "
					+ "If the identity provider is correct, add the presented value to \"registry.security.oauth2.audiences\".",
				presented,
				audiences,
			)
			OAuth2TokenValidatorResult.failure(
				OAuth2Error(INVALID_TOKEN, "The token audience $presented names none of $audiences", null)
			)
		}
	}

	@Bean
	fun jwtDecoder(jwtValidator: OAuth2TokenValidator<Jwt>): ReactiveJwtDecoder {
		return NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build().apply {
			setJwtValidator(jwtValidator)
		}
	}
}
