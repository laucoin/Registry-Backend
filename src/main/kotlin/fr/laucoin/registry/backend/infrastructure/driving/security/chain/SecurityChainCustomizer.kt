package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import org.springframework.security.config.web.server.ServerHttpSecurity

/**
 * Objectif : Contrat d'une préoccupation de sécurité appliquée à la chaîne de filtres principale.
 * Périmètre : Reçoit la configuration de sécurité HTTP, y ajoute sa part et la renvoie ; l'ordre est donné par @Order.
 * Limites : Ne construit pas la chaîne elle-même ; c'est le rôle de SecurityConfig.
 */
fun interface SecurityChainCustomizer {
	fun customize(http: ServerHttpSecurity): ServerHttpSecurity
}
