package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import fr.laucoin.registry.backend.infrastructure.driving.security.handler.AuthorizationErrorHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.CsrfTokenHeaderHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.service.CsrfTokenService
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.HEAD
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpMethod.TRACE
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.csrf.ServerCsrfTokenRequestAttributeHandler
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.stereotype.Component
import org.springframework.util.AntPathMatcher

/**
 * Objectif : Protège les requêtes modifiantes contre le CSRF et expose le jeton aux clients.
 * Périmètre : Configure le dépôt de jeton, les méthodes sûres, les chemins exemptés et le refus d'accès.
 * Limites : Ne s'applique pas aux appels portant un jeton porteur ni aux endpoints d'échange de jeton.
 */
@Component
@Order(5)
class CsrfChainCustomizer(
	private val csrfTokenService: CsrfTokenService,
	private val authorizationErrorHandler: AuthorizationErrorHandler,
) : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity {
		return http
			.addFilterBefore(CsrfTokenHeaderHandler(csrfTokenService), FIRST)
			.csrf { csrfSpec ->
				csrfSpec.csrfTokenRepository(csrfTokenService)
				csrfSpec.requireCsrfProtectionMatcher(csrfRequiredMatcher())
				csrfSpec.accessDeniedHandler(authorizationErrorHandler.accessDeniedHandler())
				csrfSpec.csrfTokenRequestHandler(ServerCsrfTokenRequestAttributeHandler())
			}
	}

	private fun csrfRequiredMatcher() = ServerWebExchangeMatcher { exchange ->
		val request = exchange.request
		val isSafeMethod = request.method in CSRF_SAFE_METHODS
		val hasBearerAuth = request.headers.getFirst(AUTHORIZATION)?.startsWith("Bearer ") == true
		val isExemptPath = CSRF_EXEMPT_PATHS.any { pathMatcher.match(it, request.path.value()) }
		if (!isSafeMethod && !hasBearerAuth && !isExemptPath) MatchResult.match() else MatchResult.notMatch()
	}

	private companion object {
		val pathMatcher = AntPathMatcher()
		val CSRF_SAFE_METHODS = setOf(GET, HEAD, OPTIONS, TRACE)
		val CSRF_EXEMPT_PATHS = listOf("/api/*/authentication/token", "/api/*/authentication/token/refresh")
	}
}
