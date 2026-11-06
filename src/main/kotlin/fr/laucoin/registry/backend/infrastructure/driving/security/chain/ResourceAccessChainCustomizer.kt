package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.POST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher.MatchResult
import org.springframework.stereotype.Component

/**
 * Objectif : Déclare les endpoints publics et exige l'authentification pour tout le reste.
 * Périmètre : Liste blanche des endpoints publics, port de supervision, formulaire et déconnexion par défaut désactivés.
 * Limites : Toute extension de la liste publique doit être signalée et approuvée (AGENTS.md §10).
 */
@Component
@Order(6)
class ResourceAccessChainCustomizer(
	@param:Value($$"${registry.server.management-port}")
	private val managementPort: Int,
) : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity {
		return http
			.authorizeExchange {
				it.matchers(managementPortMatcher()).permitAll()
				it.pathMatchers(GET, "/api/*/authentication/login/uri", "/api/*/authentication/logout/uri").permitAll()
				it.pathMatchers(POST, "/api/*/authentication/token", "/api/*/authentication/token/refresh").permitAll()
				// Libellés statiques non sensibles (enums) : publics pour éviter la résolution de l'authentification et des autorités à chaque appel
				it.pathMatchers(GET, "/api/*/metadata/**").permitAll()
				it.anyExchange().authenticated()
			}
			.formLogin { it.disable() }
			.logout { it.disable() }
	}

	private fun managementPortMatcher() = ServerWebExchangeMatcher { exchange ->
		if (exchange.request.localAddress?.port == managementPort) MatchResult.match() else MatchResult.notMatch()
	}
}
