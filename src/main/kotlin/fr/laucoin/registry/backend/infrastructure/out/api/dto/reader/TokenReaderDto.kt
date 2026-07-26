package fr.laucoin.registry.backend.infrastructure.out.api.dto.reader

data class TokenReaderDto(
	var accessToken: String? = null,
	var expiresIn: Long = 0,
	var refreshExpiresIn: Long = 0,
	var refreshToken: String? = null,
	var tokenType: String? = null,
)
