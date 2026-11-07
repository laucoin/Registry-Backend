package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import java.time.Duration
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.stereotype.Component

/**
 * Objectif : Pose les en-têtes de sécurité défensifs des réponses (CSP, permissions, HSTS).
 * Périmètre : Sert la chaîne principale (CSP stricte de l'API) et la chaîne de documentation (sans CSP ici).
 * Limites : Ne décide d'aucune autorisation.
 */
@Component
@Order(2)
class SecurityHeadersChainCustomizer : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity = configure(http, API_CONTENT_SECURITY_POLICY)

	fun configure(http: ServerHttpSecurity, contentSecurityPolicy: String?): ServerHttpSecurity = http.headers { headerSpec ->
		if (contentSecurityPolicy != null) headerSpec.contentSecurityPolicy { it.policyDirectives(contentSecurityPolicy) }
		headerSpec.permissionsPolicy { it.policy(PERMISSIONS_POLICY) }
		headerSpec.hsts {
			it.maxAge(Duration.ofDays(365))
			it.includeSubdomains(true)
			it.preload(true)
		}
	}

	private companion object {
		const val API_CONTENT_SECURITY_POLICY = "default-src 'none'; frame-ancestors 'none'"

		const val PERMISSIONS_POLICY =
			"geolocation=(), camera=(), microphone=(), payment=(), usb=(), interest-cohort=()"
	}
}
