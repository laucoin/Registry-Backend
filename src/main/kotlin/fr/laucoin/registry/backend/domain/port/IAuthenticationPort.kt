package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import reactor.core.publisher.Mono

/**
 * Persistence-agnostic port for the OAuth2/OIDC flow: building login/logout URIs and exchanging or
 * refreshing tokens against the identity provider. Implemented by the Authentik (`driven/idp`)
 * adapter; the domain never talks to the IDP's HTTP API directly.
 */
interface IAuthenticationPort {
	fun getLoginUri(redirectUri: String): AuthenticationUriModel
	fun getLogoutUri(redirectUri: String, accessToken: String?, refreshToken: String?): Mono<AuthenticationUriModel>
	fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel>
	fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel>
}
