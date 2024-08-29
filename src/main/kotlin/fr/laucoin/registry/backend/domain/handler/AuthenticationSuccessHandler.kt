package fr.laucoin.registry.backend.domain.handler

import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.FOUND
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


@Component
class AuthenticationSuccessHandler(
    @Value("\${external.frontend.base-url}")
    private val frontendUrl: String,
): ServerAuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(webFilterExchange: WebFilterExchange, authentication: Authentication): Mono<Void> {
        val exchange: ServerWebExchange = webFilterExchange.exchange

        return Mono.fromRunnable {
            exchange.response.setStatusCode(FOUND)
            exchange.response.headers.location = URI.create("$frontendUrl/auth-callback")
        }
    }
}
