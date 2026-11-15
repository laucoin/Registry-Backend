package fr.laucoin.registry.backend.infrastructure.driving.security.handler

import fr.laucoin.registry.backend.infrastructure.driving.security.service.AuthenticationCookieService.Companion.ACCESS_TOKEN_COOKIE
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Objectif : Lit le bearer token depuis l'en-tête Authorization ou le cookie d'accès.
 * Périmètre : Privilégie l'en-tête et se rabat sur le cookie du token d'accès, sauf sur les endpoints publics
 * d'authentification où un cookie périmé ferait échouer la connexion.
 * Limites : Ne valide pas le token.
 */
@Component
class CookieBearerTokenHandler : ServerAuthenticationConverter {
	private val headerConverter = ServerBearerTokenAuthenticationConverter()

	override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
		return headerConverter.convert(exchange)
			.switchIfEmpty { convertFromCookie(exchange) }
	}

	private fun convertFromCookie(exchange: ServerWebExchange): Mono<Authentication> {
		val path = exchange.request.path.value()
		if (COOKIE_FREE_PATHS.any { pathMatcher.match(it, path) }) return Mono.empty()
		val accessToken = exchange.request.cookies.getFirst(ACCESS_TOKEN_COOKIE)?.value
			?: return Mono.empty()
		return Mono.just(BearerTokenAuthenticationToken(accessToken))
	}

	private companion object {
		val pathMatcher = AntPathMatcher()
		val COOKIE_FREE_PATHS = listOf(
			"/api/*/authentication/login/uri",
			"/api/*/authentication/logout/uri",
			"/api/*/authentication/token",
			"/api/*/authentication/token/refresh",
		)
	}
}
