package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_NOT_ALLOWED
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.domain.service.IAuthenticationService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Protège les URI de redirection transmises au fournisseur d'identité avant de déléguer au port d'authentification.
 * Périmètre : Porte les listes autorisées à correspondance exacte pour le flux de connexion/token et pour le flux de déconnexion.
 * Limites : Ne dialogue pas lui-même avec le fournisseur d'identité et ne gère ni cookies ni sessions.
 */
@Service
class AuthenticationService(
	private val port: IAuthenticationPort,
	@Value($$"${external.idp.allowed-redirect-uris}")
	configuredRedirectUris: List<String>,
	@Value($$"${external.idp.allowed-logout-redirect-uris}")
	configuredLogoutRedirectUris: List<String>,
) : IAuthenticationService, LoggerService() {
	private val allowedRedirectUris: List<String> = configuredRedirectUris.filter { it.isNotBlank() }
	private val allowedLogoutRedirectUris: List<String> = configuredLogoutRedirectUris.filter { it.isNotBlank() }

	init {
		require(allowedRedirectUris.isNotEmpty()) { "external.idp.allowed-redirect-uris must not be empty" }
		require(allowedLogoutRedirectUris.isNotEmpty()) { "external.idp.allowed-logout-redirect-uris must not be empty" }
		warnOnSuspiciousEntries("external.idp.allowed-redirect-uris", allowedRedirectUris)
		warnOnSuspiciousEntries("external.idp.allowed-logout-redirect-uris", allowedLogoutRedirectUris)
	}

	override fun getLoginUri(redirectUri: String, state: String?): AuthenticationUriModel {
		if (redirectUri !in allowedRedirectUris) throw notAllowed()
		return port.getLoginUri(redirectUri, state)
	}

	override fun getLogoutUri(
		redirectUri: String,
		accessToken: String?,
		refreshToken: String?,
	): Mono<AuthenticationUriModel> {
		return checkRedirectUri(redirectUri, allowedLogoutRedirectUris)
			.then(Mono.defer { port.getLogoutUri(redirectUri, accessToken, refreshToken) })
	}

	override fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel> {
		return checkRedirectUri(redirectUri, allowedRedirectUris)
			.then(Mono.defer { port.getAuthenticationToken(authorizationCode, redirectUri) })
	}

	override fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel> {
		return port.refreshAuthenticationToken(refreshToken)
	}

	private fun warnOnSuspiciousEntries(key: String, entries: List<String>) {
		if (entries.any { it.endsWith("/") || it.contains("*") }) {
			log.warn("{} holds an entry ending with '/' or containing '*': entries are matched exactly, no wildcard is supported", key)
		}
	}

	private fun checkRedirectUri(redirectUri: String, allowed: List<String>): Mono<Void> {
		return if (redirectUri in allowed) Mono.empty() else Mono.error(notAllowed())
	}

	private fun notAllowed() = RegistryException(BAD_REQUEST, REDIRECT_URI_NOT_ALLOWED)
}
