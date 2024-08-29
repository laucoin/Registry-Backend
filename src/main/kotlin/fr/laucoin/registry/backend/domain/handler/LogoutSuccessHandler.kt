package fr.laucoin.registry.backend.domain.handler

import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FOUND
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class LogoutSuccessHandler(
    @Value("\${registry.security.oauth2.url}/logout")
    private val authProviderLogoutUrl: String,
    @Value("\${registry.security.oauth2.client-id}")
    private val authProviderClientId: String,
    @Value("\${external.frontend.base-url}/sign-out-callback")
    private val frontendRedirectUri: String
): ServerLogoutSuccessHandler {
    override fun onLogoutSuccess(webFilterExchange: WebFilterExchange, authentication: Authentication): Mono<Void> {
        val exchange: ServerWebExchange = webFilterExchange.exchange

        return Mono.fromRunnable {
            exchange.response.setStatusCode(FOUND)
            exchange.response.headers.location = URI.create(
                "$authProviderLogoutUrl?client_id=$authProviderClientId&post_logout_redirect_uri=$frontendRedirectUri"
            )
        }
    }
}
