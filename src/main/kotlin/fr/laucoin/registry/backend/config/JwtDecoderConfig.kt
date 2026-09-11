package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.domain.validator.JwtAudienceValidator
import fr.laucoin.registry.backend.domain.validator.JwtIssuerOriginValidator
import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder

/**
 * Spring Boot's autoconfigured decoder (driven by `jwk-set-uri` alone) only checks the JWT's
 * signature and timestamps. This bean adds the audience and issuer checks it skips, closing the
 * confused-deputy gap where a token minted by the same Authentik instance for an unrelated
 * application could otherwise be accepted here.
 */
@Configuration
class JwtDecoderConfig(
	@param:Value($$"${registry.security.oauth2.jwks-uri}")
	private val jwksUri: String,
	@param:Value($$"${registry.security.oauth2.authorization-uri}")
	private val authorizationUri: String,
	@param:Value($$"${registry.security.oauth2.client-id}")
	private val privateClientId: String,
	@param:Value($$"${external.idp.swagger.client-id}")
	private val publicClientId: String,
) {

	@Bean
	fun jwtDecoder(): ReactiveJwtDecoder {
		val decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build()
		val trustedOrigin = URI(authorizationUri).let { "${it.scheme}://${it.authority}" }

		decoder.setJwtValidator(
			DelegatingOAuth2TokenValidator(
				JwtValidators.createDefault(),
				JwtAudienceValidator(setOf(privateClientId, publicClientId)),
				JwtIssuerOriginValidator(trustedOrigin),
			)
		)

		return decoder
	}
}
