package fr.laucoin.registry.backend.config

import fr.laucoin.registry.backend.infrastructure.driving.security.service.PermissionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity

/**
 * Objectif : Active la sécurité par méthode réactive et branche l'évaluateur de permissions du projet.
 * Périmètre : Fournit le gestionnaire d'expressions utilisé par les annotations d'autorisation des contrats.
 * Limites : Ne décide d'aucune autorisation ; elles sont déclarées sur les contrats.
 */
@Configuration
@EnableReactiveMethodSecurity
class MethodSecurityConfig {
	@Bean
	fun expressionHandler(): MethodSecurityExpressionHandler {
		val handler = DefaultMethodSecurityExpressionHandler()
		handler.setPermissionEvaluator(PermissionService())
		return handler
	}
}
