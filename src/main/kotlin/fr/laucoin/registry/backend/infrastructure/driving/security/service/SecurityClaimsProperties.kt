package fr.laucoin.registry.backend.infrastructure.driving.security.service

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Objectif : Porte la configuration typée des noms de claims du JWT émis par le fournisseur d'identité.
 * Périmètre : Noms des claims identifiant, email, email vérifié, prénom et nom.
 * Limites : Ne lit aucun jeton ; les services d'authentification résolvent les claims avec ces noms.
 */
@ConfigurationProperties("registry.security.oauth2.claims")
data class SecurityClaimsProperties(
	val userId: String,
	val email: String,
	val emailVerified: String,
	val firstName: String,
	val lastName: String,
)
