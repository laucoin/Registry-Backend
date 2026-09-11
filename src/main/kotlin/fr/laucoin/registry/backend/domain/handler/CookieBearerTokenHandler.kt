package fr.laucoin.registry.backend.domain.handler

import fr.laucoin.registry.backend.domain.service.impl.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Component
class CookieBearerTokenHandler : ServerAuthenticationConverter {
	private val headerConverter = ServerBearerTokenAuthenticationConverter()

	override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
		return headerConverter.convert(exchange)
			.switchIfEmpty { convertFromCookie(exchange) }
	}

	private fun convertFromCookie(exchange: ServerWebExchange): Mono<Authentication> {
		val accessToken = exchange.request.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.value
			?: return Mono.empty()
		return Mono.just(BearerTokenAuthenticationToken(accessToken))
	}
}
