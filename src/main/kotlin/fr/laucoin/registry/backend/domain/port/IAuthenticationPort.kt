package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import reactor.core.publisher.Mono

interface IAuthenticationPort {
	fun getLoginUri(redirectUri: String): AuthenticationUriModel
	fun getLogoutUri(redirectUri: String, accessToken: String?, refreshToken: String?): Mono<AuthenticationUriModel>
	fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel>
	fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel>
}
