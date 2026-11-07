package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.infrastructure.driving.security.service.SecurityClaimsProperties
import fr.laucoin.registry.backend.infrastructure.driving.security.chain.SecurityChainCustomizer
import fr.laucoin.registry.backend.infrastructure.driving.security.chain.SecurityRateLimitProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

/**
 * Objectif : Assemble la chaîne de filtres de sécurité principale de l'API.
 * Périmètre : Applique dans l'ordre les préoccupations de sécurité (langue, en-têtes, débit, audit, CSRF, accès, OAuth2).
 * Limites : Ne porte aucune autorisation métier ; la sécurité par méthode sur les contrats décide endpoint par endpoint.
 */
@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(SecurityRateLimitProperties::class, SecurityClaimsProperties::class)
class SecurityConfig(private val customizers: List<SecurityChainCustomizer>) {
	@Bean
	@Order(2)
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
		return customizers.fold(http) { secured, customizer -> customizer.customize(secured) }.build()
	}
}
