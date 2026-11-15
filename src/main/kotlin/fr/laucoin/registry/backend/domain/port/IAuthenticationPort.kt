package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import reactor.core.publisher.Mono

/**
 * Objectif : Port vers le fournisseur d'identité externe pour l'authentification.
 * Périmètre : Déclare la construction des URI de connexion et de déconnexion, l'échange et le rafraîchissement des jetons.
 * Limites : Contrat uniquement ; l'adaptateur porte le dialogue avec le fournisseur.
 */
interface IAuthenticationPort {
	fun getLoginUri(redirectUri: String, state: String?): AuthenticationUriModel
	fun getLogoutUri(redirectUri: String, accessToken: String?, refreshToken: String?): Mono<AuthenticationUriModel>
	fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel>
	fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel>
}
