package fr.laucoin.registry.backend.infrastructure.`in`.keycloak.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.`in`.keycloak.entity.KeycloakTokenEntity
import fr.laucoin.registry.backend.infrastructure.`in`.keycloak.mapper.AuthenticationTokenEntityMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FAILED_DEPENDENCY
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.BodyInserters.FormInserter
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class KeycloakAuthenticationAdapter(
	private val mapper: AuthenticationTokenEntityMapper,
	@param:Value($$"${external.oidc.authorization-uri}")
	private val authorizationUri: String,
	@param:Value($$"${external.oidc.token-uri}")
	private val tokenUri: String,
	@param:Value($$"${external.oidc.end-session-uri}")
	private val endSessionUri: String,
	@param:Value($$"${external.oidc.client-id}")
	private val clientId: String,
	@param:Value($$"${external.oidc.client-secret}")
	private val clientSecret: String,
): IAuthenticationPort {
	private val http: WebClient = WebClient.create()

	private companion object {
		private const val RESPONSE_TYPE = "code"
	}

	override fun getLoginUri(redirectUri: String): AuthenticationUriModel {
		return AuthenticationUriModel(
			uri = "$authorizationUri?response_type=$RESPONSE_TYPE&client_id=$clientId&redirect_uri=$redirectUri"
		)
	}

	override fun getLogoutUri(redirectUri: String): AuthenticationUriModel {
		return AuthenticationUriModel(
			uri = "$endSessionUri?redirect_uri=$redirectUri"
		)
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
		return http.post().uri(tokenUri)
			.body(
				body
					.with("client_id", clientId)
					.with("client_secret", clientSecret)
			)
			.retrieve()
			.onStatus(
				{ it.is4xxClientError },
				{ Mono.error(RegistryException(UNAUTHORIZED, errorMessage)) })
			.onStatus(
				{ it.is5xxServerError },
				{ Mono.error(RegistryException(FAILED_DEPENDENCY, AUTH_PROVIDER_FAILED)) })
			.bodyToMono(KeycloakTokenEntity::class.java)
			.map(mapper::toModel)
	}
}
