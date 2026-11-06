package fr.laucoin.registry.backend.infrastructure.driven.idp

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Objectif : Porte la configuration typée de l'accès au fournisseur d'identité externe.
 * Périmètre : URI des endpoints, identifiants du client privé, délais et taille du pool de connexions.
 * Limites : Ne contacte pas le fournisseur ; les autres clés `external.idp` sont lues par leurs propres consommateurs.
 */
@ConfigurationProperties("external.idp")
data class IdpProperties(
	val authorizationUri: String,
	val tokenUri: String,
	val endSessionUri: String,
	val revocationUri: String,
	val clientId: String,
	val clientSecret: String,
	val connectTimeoutMillis: Int,
	val responseTimeoutMillis: Long,
	val maxConnections: Int,
)
