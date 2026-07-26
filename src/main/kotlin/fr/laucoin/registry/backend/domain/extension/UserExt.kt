package fr.laucoin.registry.backend.domain.extension

import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

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

	/**
	 * Reads a boolean claim without ever throwing: an absent claim, or a value the
	 * conversion service cannot read as a boolean, both yield `false`. Callers use
	 * this for security decisions, so an unreadable assertion must never be taken
	 * as a positive one.
	 */
	fun Jwt.getClaimAsBooleanOrFalse(claim: String): Boolean {
		return if (!hasClaim(claim)) false
		else {
			try {
				getClaimAsBoolean(claim) == true
			} catch (e: Exception) {
				false
			}
		}
	}
}
