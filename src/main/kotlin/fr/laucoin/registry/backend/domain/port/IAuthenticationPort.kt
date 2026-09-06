package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.AuthorizationChallengeModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import reactor.core.publisher.Mono

interface IAuthenticationPort {
	fun getLoginUri(redirectUri: String, challenge: AuthorizationChallengeModel): AuthenticationUriModel
	fun getLogoutUri(redirectUri: String): AuthenticationUriModel
	fun getAuthenticationToken(
		authorizationCode: String,
		redirectUri: String,
		codeVerifier: String,
	): Mono<TokenModel>

	fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel>
}
