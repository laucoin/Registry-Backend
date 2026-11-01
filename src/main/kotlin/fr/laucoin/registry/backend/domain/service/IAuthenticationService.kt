package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage du flux d'authentification.
 * Périmètre : Construit les URI de connexion et de déconnexion et échange ou rafraîchit les tokens, après vérification des URI de redirection.
 * Limites : Contrat uniquement ; l'implémentation porte les listes autorisées.
 */
interface IAuthenticationService {
	fun getLoginUri(redirectUri: String, state: String?): AuthenticationUriModel
	fun getLogoutUri(redirectUri: String, accessToken: String?, refreshToken: String?): Mono<AuthenticationUriModel>
	fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel>
	fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel>
}
