package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.infrastructure.driving.security.validator.JwtAudienceValidator
import fr.laucoin.registry.backend.infrastructure.driving.security.validator.JwtIssuerOriginValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.net.URI

/**
 * Objectif : Construit le décodeur JWT du resource server.
 * Périmètre : Relie la source JWKS aux validateurs d'audience et d'origine de l'émetteur.
 * Limites : Ne mappe pas les claims vers des autorités ; le convertisseur de token s'en charge.
 */
@Configuration
class JwtDecoderConfig(
	@param:Value($$"${registry.security.oauth2.jwks-uri}")
	private val jwkUri: String,
	@param:Value($$"${registry.security.oauth2.authorization-uri}")
	private val authorizationUri: String,
	@param:Value($$"${registry.security.oauth2.client-id}")
	private val privateClientId: String,
	@param:Value($$"${external.idp.swagger.client-id}")
	private val publicClientId: String,
) {

	@Bean
	fun jwtDecoder(): ReactiveJwtDecoder {
		val decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkUri).build()
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
