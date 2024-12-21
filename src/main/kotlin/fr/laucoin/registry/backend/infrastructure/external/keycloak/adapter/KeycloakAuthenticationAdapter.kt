package fr.laucoin.registry.backend.infrastructure.external.keycloak.adapter

import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTHORIZATION_CODE_OUTDATED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.AUTH_PROVIDER_FAILED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.AuthError.REFRESH_TOKEN_OUTDATED
import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.model.TokenModel
import fr.laucoin.registry.backend.domain.port.IAuthenticationPort
import fr.laucoin.registry.backend.infrastructure.external.keycloak.entity.KeycloakTokenEntity
import fr.laucoin.registry.backend.infrastructure.external.keycloak.mapper.AuthenticationTokenEntityMapper
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
    @Value("\${external.keycloak.base-url}/realms/\${external.keycloak.realm}")
    private val baseUri: String,
    @Value("\${external.keycloak.client-id}")
    private val clientId: String,
    @Value("\${external.keycloak.client-secret}")
    private val clientSecret: String,
): IAuthenticationPort {
    private val http: WebClient = WebClient.create(baseUri)

    companion object {
        private const val BASE_PATH = "/protocol/openid-connect"
        private const val AUTHENTICATION_PATH = "$BASE_PATH/auth"
        private const val TOKEN_PATH = "$BASE_PATH/token"
        private const val LOGOUT_PATH = "$BASE_PATH/logout"

        private const val RESPONSE_TYPE = "code"
    }

    override fun getLoginUri(redirectUri: String): AuthenticationUriModel {
        return AuthenticationUriModel(
            uri = "$baseUri$AUTHENTICATION_PATH?response_type=$RESPONSE_TYPE&client_id=$clientId&redirect_uri=$redirectUri"
        )
    }

    override fun getLogoutUri(redirectUri: String): AuthenticationUriModel {
        return AuthenticationUriModel(
            uri = "$baseUri$LOGOUT_PATH?redirect_uri=$redirectUri"
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
        return http.post().uri("$baseUri$TOKEN_PATH")
            .body(
                body
                    .with("client_id", clientId)
                    .with("client_secret", clientSecret)
            )
            .retrieve()
            .onStatus(
                { it.is4xxClientError },
                { Mono.error(RegistryExceptionModel(UNAUTHORIZED, errorMessage)) })
            .onStatus({ it.is5xxServerError }, { Mono.error(RegistryExceptionModel(FAILED_DEPENDENCY, AUTH_PROVIDER_FAILED)) })
            .bodyToMono(KeycloakTokenEntity::class.java)
            .map(mapper::toModel)
    }
}
