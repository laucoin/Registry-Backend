package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

/**
 * What the token endpoints return now that the tokens themselves travel in `HttpOnly` cookies.
 *
 * Only the lifetimes are exposed, because the frontend still needs them to schedule its renewal —
 * it can no longer read the tokens to inspect their expiry, which is the point.
 */
data class SessionReaderDto(
	val expiresIn: Long,
	/** Null when the provider stated no refresh lifetime, or issued no refresh token at all. */
	val refreshExpiresIn: Long? = null,
)
