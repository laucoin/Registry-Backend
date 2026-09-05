package fr.laucoin.registry.backend.domain.model

data class TokenModel(
	val accessToken: String,
	val expiresIn: Long,
	/** Absent on providers that do not advertise a refresh lifetime — Authentik, for one. */
	val refreshExpiresIn: Long? = null,
	/** Absent when the provider issued no refresh token, typically for want of `offline_access`. */
	val refreshToken: String? = null,
	val tokenType: String,
)
