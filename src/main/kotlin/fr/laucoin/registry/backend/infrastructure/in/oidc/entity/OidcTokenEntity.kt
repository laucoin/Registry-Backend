package fr.laucoin.registry.backend.infrastructure.`in`.oidc.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class OidcTokenEntity(
	@field:JsonProperty("access_token")
	val accessToken: String,
	@field:JsonProperty("expires_in")
	val expiresIn: Long,
	@field:JsonProperty("refresh_expires_in")
	val refreshExpiresIn: Long,
	@field:JsonProperty("refresh_token")
	val refreshToken: String,
	@field:JsonProperty("token_type")
	val tokenType: String,
)
