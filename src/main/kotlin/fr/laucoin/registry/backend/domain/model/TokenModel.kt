package fr.laucoin.registry.backend.domain.model

data class TokenModel(
	val accessToken: String,
	val expiresIn: Long,
	val refreshToken: String,
	val tokenType: String,
)
