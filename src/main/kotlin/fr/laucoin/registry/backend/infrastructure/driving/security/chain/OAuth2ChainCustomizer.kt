package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import fr.laucoin.registry.backend.infrastructure.driving.security.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.CookieBearerTokenHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.service.TokenConverterService
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.stereotype.Component

/**
 * Objectif : Configure le serveur de ressources OAuth2 (JWT) et la gestion des erreurs d'accès.
 * Périmètre : Convertisseur de jeton, lecture du jeton en cookie, refus d'accès et entrée non authentifiée.
 * Limites : Ne stocke aucun mot de passe ; l'authentification reste déléguée à l'IDP.
 */
@Component
@Order(7)
class OAuth2ChainCustomizer(
	private val tokenConverter: TokenConverterService,
	private val authorizationErrorHandler: AuthorizationErrorHandler,
	private val cookieBearerTokenHandler: CookieBearerTokenHandler,
) : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity {
		return http
			.oauth2ResourceServer { resourceServer ->
				resourceServer.authenticationFailureHandler(authorizationErrorHandler)
				resourceServer.bearerTokenConverter(cookieBearerTokenHandler)
				resourceServer.jwt { it.jwtAuthenticationConverter(tokenConverter) }
			}
			.exceptionHandling {
				it.accessDeniedHandler(authorizationErrorHandler.accessDeniedHandler())
				it.authenticationEntryPoint(authorizationErrorHandler.unauthorizedHandler())
			}
	}
}
