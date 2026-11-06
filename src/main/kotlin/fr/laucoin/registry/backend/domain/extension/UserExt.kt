package fr.laucoin.registry.backend.domain.extension

import java.util.UUID
import org.springframework.security.oauth2.jwt.Jwt

/**
 * Objectif : Fournit des extensions sur le jeton JWT de l'utilisateur.
 * Périmètre : Lit un claim sous forme d'UUID.
 * Limites : Retourne `null` pour un claim absent ou invalide, sans lever d'erreur.
 */
object UserExt {
	fun Jwt.getClaimAsUUID(claim: String): UUID? {
		return if (!hasClaim(claim)) null
		else {
			try {
				UUID.fromString(getClaimAsString(claim))
			} catch (e: Exception) {
				null
			}
		}
	}
}
