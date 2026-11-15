package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import fr.laucoin.registry.backend.domain.service.support.IAuditReadService
import fr.laucoin.registry.backend.infrastructure.driving.security.handler.AuditReadHandler
import fr.laucoin.registry.backend.infrastructure.driving.security.service.ClientContextService
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.AUTHENTICATION
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.stereotype.Component

/**
 * Objectif : Trace les lectures de données personnelles après l'authentification.
 * Périmètre : Ajoute le filtre d'audit des lectures à la chaîne principale.
 * Limites : N'enregistre rien lui-même ; le filtre délègue au service d'audit.
 */
@Component
@Order(4)
class AuditReadChainCustomizer(
	private val auditReadService: IAuditReadService,
	private val clientContextService: ClientContextService,
) : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity {
		return http.addFilterAfter(AuditReadHandler(auditReadService, clientContextService), AUTHENTICATION)
	}
}
