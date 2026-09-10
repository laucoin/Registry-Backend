package fr.laucoin.registry.backend.infrastructure.`in`.keycloak.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REDIRECT_URI_NOT_ALLOWED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.AuthorizationChallengeModel
import fr.laucoin.registry.backend.domain.model.AuthorizationChallengeModel.Companion.CHALLENGE_METHOD
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.`in`.keycloak.entity.KeycloakTokenEntity
import fr.laucoin.registry.backend.infrastructure.`in`.keycloak.mapper.AuthenticationTokenEntityMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FAILED_DEPENDENCY
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.BodyInserters.FormInserter
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.URI

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
	@param:Value($$"${external.cors.urls}")
	private val allowedOrigins: List<String>,
) : IAuthenticationPort {
	private val http: WebClient = WebClient.create()
	private val log = LoggerFactory.getLogger(this::class.java)

	private companion object {
		private const val RESPONSE_TYPE = "code"
		private const val SCOPE = "openid profile email offline_access"
	}

	override fun getLoginUri(redirectUri: String, challenge: AuthorizationChallengeModel): AuthenticationUriModel {
		validateRedirectUri(redirectUri)
		return AuthenticationUriModel(
			uri = UriComponentsBuilder.fromUriString(authorizationUri)
				.queryParam("response_type", RESPONSE_TYPE)
				.queryParam("client_id", clientId)
				.queryParam("scope", SCOPE)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("state", challenge.state)
				.queryParam("nonce", challenge.nonce)
				.queryParam("code_challenge", challenge.codeChallenge)
				.queryParam("code_challenge_method", CHALLENGE_METHOD)
				.build()
				.encode()
				.toUriString()
		)
	}

	override fun getLogoutUri(redirectUri: String): AuthenticationUriModel {
		validateRedirectUri(redirectUri)
		return AuthenticationUriModel(
			uri = UriComponentsBuilder.fromUriString(endSessionUri)
				.queryParam("redirect_uri", redirectUri)
				.build()
				.encode()
				.toUriString()
		)
	}

	private fun validateRedirectUri(redirectUri: String) {
		val origin = originOf(redirectUri)
		if (origin == null || allowedOrigins.none { originOf(it) == origin }) {
			log.warn("Refusing redirect URI \"{}\": its origin is not among {}", redirectUri, allowedOrigins)
			throw RegistryException(BAD_REQUEST, REDIRECT_URI_NOT_ALLOWED)
		}
	}

	private fun originOf(value: String): String? = runCatching {
		val uri = URI(value.trim())
		if (uri.scheme == null || uri.host == null) null
		else "${uri.scheme.lowercase()}://${uri.host.lowercase()}${if (uri.port == -1) "" else ":${uri.port}"}"
	}.getOrNull()

	override fun getAuthenticationToken(
		authorizationCode: String,
		redirectUri: String,
		codeVerifier: String,
	): Mono<TokenModel> {
		return fetchToken(
			BodyInserters.fromFormData("grant_type", "authorization_code")
				.with("code", authorizationCode)
				.with("redirect_uri", redirectUri)
				.with("code_verifier", codeVerifier),
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
			.bodyToMono<KeycloakTokenEntity>()
			.map(mapper::toModel)
	}
}
