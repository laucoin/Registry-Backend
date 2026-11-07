package fr.laucoin.registry.backend.infrastructure.driven.idp.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.domain.service.LoggerService
import fr.laucoin.registry.backend.infrastructure.driven.idp.IdpProperties
import fr.laucoin.registry.backend.infrastructure.driven.idp.entity.IdpTokenEntity
import fr.laucoin.registry.backend.infrastructure.driven.idp.mapper.AuthenticationTokenEntityMapper
import io.netty.channel.ChannelOption
import java.net.URLEncoder
import java.time.Duration
import kotlin.text.Charsets.UTF_8
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpStatus.FAILED_DEPENDENCY
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.BodyInserters.FormInserter
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider

/**
 * Objectif : Dialogue avec le fournisseur d'identité externe.
 * Périmètre : Construit les URI de connexion et de déconnexion, échange, rafraîchit et révoque les tokens.
 * Limites : Ne stocke rien ; les listes autorisées d'URI de redirection sont vérifiées par le service.
 */
@Service
@EnableConfigurationProperties(IdpProperties::class)
class IdpAuthenticationAdapter(
	private val mapper: AuthenticationTokenEntityMapper,
	private val idp: IdpProperties,
) : IAuthenticationPort, LoggerService() {
	private val http: WebClient = buildHttpClient(idp.connectTimeoutMillis, idp.responseTimeoutMillis, idp.maxConnections)

	private companion object {
		private const val RESPONSE_TYPE = "code"
		private const val CONNECTION_PROVIDER_NAME = "idp-http-client"

		private fun buildHttpClient(
			connectTimeoutMillis: Int,
			responseTimeoutMillis: Long,
			maxConnections: Int,
		): WebClient {
			val connectionProvider = ConnectionProvider.builder(CONNECTION_PROVIDER_NAME)
				.maxConnections(maxConnections)
				.pendingAcquireTimeout(Duration.ofMillis(responseTimeoutMillis))
				.build()

			val httpClient = HttpClient.create(connectionProvider)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
				.responseTimeout(Duration.ofMillis(responseTimeoutMillis))

			return WebClient.builder()
				.clientConnector(ReactorClientHttpConnector(httpClient))
				.build()
		}
	}

	override fun getLoginUri(redirectUri: String, state: String?): AuthenticationUriModel {
		val stateParameter = state?.let { "&state=${it.urlEncoded()}" }.orEmpty()
		return AuthenticationUriModel(
			uri = "${idp.authorizationUri}?response_type=$RESPONSE_TYPE&client_id=${idp.clientId}&redirect_uri=${redirectUri.urlEncoded()}$stateParameter"
		)
	}

	override fun getLogoutUri(
		redirectUri: String,
		accessToken: String?,
		refreshToken: String?
	): Mono<AuthenticationUriModel> {
		return revokeTokens(accessToken, refreshToken)
			.thenReturn(AuthenticationUriModel(uri = "${idp.endSessionUri}?redirect_uri=${redirectUri.urlEncoded()}"))
	}

	private fun revokeTokens(accessToken: String?, refreshToken: String?): Mono<Void> {
		return Mono.`when`(
			revokeToken(accessToken, "access_token"),
			revokeToken(refreshToken, "refresh_token"),
		)
	}

	private fun String.urlEncoded(): String = URLEncoder.encode(this, UTF_8)

	private fun revokeToken(token: String?, tokenTypeHint: String): Mono<Void> {
		if (token.isNullOrBlank()) return Mono.empty()

		return http.post().uri(idp.revocationUri)
			.body(
				BodyInserters.fromFormData("token", token)
					.with("token_type_hint", tokenTypeHint)
					.with("client_id", idp.clientId)
					.with("client_secret", idp.clientSecret)
			)
			.retrieve()
			.toBodilessEntity()
			.doOnError { log.warn("Failed to revoke the {} at the identity provider during logout", tokenTypeHint, it) }
			.onErrorResume { Mono.empty() }
			.then()
	}

	override fun getAuthenticationToken(authorizationCode: String, redirectUri: String): Mono<TokenModel> {
		return fetchToken(
			BodyInserters.fromFormData("grant_type", "authorization_code")
				.with("code", authorizationCode)
				.with("redirect_uri", redirectUri),
			AUTHORIZATION_CODE_OUTDATED
		)
	}

	override fun refreshAuthenticationToken(refreshToken: String): Mono<TokenModel> {
		return fetchToken(
			BodyInserters.fromFormData("grant_type", "refresh_token")
				.with("refresh_token", refreshToken),
			REFRESH_TOKEN_OUTDATED
		)
	}

	private fun fetchToken(body: FormInserter<String>, errorMessage: String): Mono<TokenModel> {
		return http.post().uri(idp.tokenUri)
			.body(
				body
					.with("client_id", idp.clientId)
					.with("client_secret", idp.clientSecret)
			)
			.retrieve()
			.onStatus(
				{ it.is4xxClientError },
				{ Mono.error(RegistryException(UNAUTHORIZED, errorMessage)) })
			.onStatus(
				{ it.is5xxServerError },
				{ Mono.error(RegistryException(FAILED_DEPENDENCY, AUTH_PROVIDER_FAILED)) })
			.bodyToMono(IdpTokenEntity::class.java)
			.map(mapper::toModel)
	}
}
