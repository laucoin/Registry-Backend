package fr.laucoin.registry.backend.domain.extension

import java.util.UUID
import org.springframework.security.oauth2.jwt.Jwt

/**
 * [getClaimAsUUID] safely reads a [Jwt] claim as a [UUID], returning `null` on a missing or
 * malformed claim instead of throwing.
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
