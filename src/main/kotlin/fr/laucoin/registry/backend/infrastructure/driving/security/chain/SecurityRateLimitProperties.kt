package fr.laucoin.registry.backend.infrastructure.driving.security.chain

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Objectif : Porte la configuration typée de la limitation de débit (authentification, export, recherche d'utilisateurs et création de projets).
 * Périmètre : Capacité, fenêtre en secondes et nombre maximal de clients ou d'utilisateurs suivis, par type de limite.
 * Limites : Ne limite rien lui-même ; les filtres de sécurité appliquent ces valeurs.
 */
@ConfigurationProperties("registry.security.rate-limit")
data class SecurityRateLimitProperties(
	val auth: ClientLimit,
	val export: UserLimit,
	val userSearch: UserLimit,
	val projectCreation: UserLimit,
) {
	data class ClientLimit(val capacity: Int, val windowSeconds: Long, val maxTrackedClients: Long)

	data class UserLimit(val capacity: Int, val windowSeconds: Long, val maxTrackedUsers: Long)
}
