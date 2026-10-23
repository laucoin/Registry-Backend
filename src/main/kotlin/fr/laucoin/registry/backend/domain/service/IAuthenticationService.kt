package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import reactor.core.publisher.Mono

/**
 * Purpose: Use-case contract of the authentication flow.
 * Scope: Builds login and logout URIs and exchanges or refreshes tokens, after checking the redirect URIs.
 * Limits: Contract only; the implementation holds the allow-lists.
 */
interface IAuthenticationService {
	fun getLoginUri(redirectUri: String): AuthenticationUriModel
	fun getLogoutUri(redirectUri: String, accessToken: String?, refreshToken: String?): Mono<AuthenticationUriModel>
	fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel>
	fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel>
}
