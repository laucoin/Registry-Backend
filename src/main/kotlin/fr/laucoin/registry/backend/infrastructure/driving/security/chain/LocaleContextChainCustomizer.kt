package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import fr.laucoin.registry.backend.infrastructure.driving.security.handler.LocaleContextHandler
import org.springframework.core.annotation.Order
import org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.stereotype.Component

/**
 * Objectif : Résout la langue de la requête avant tout autre filtre de la chaîne principale.
 * Périmètre : Ajoute le filtre de contexte de langue en première position.
 * Limites : Ne traduit aucun message.
 */
@Component
@Order(1)
class LocaleContextChainCustomizer(private val localeContextHandler: LocaleContextHandler) : SecurityChainCustomizer {
	override fun customize(http: ServerHttpSecurity): ServerHttpSecurity = http.addFilterBefore(localeContextHandler, FIRST)
}
