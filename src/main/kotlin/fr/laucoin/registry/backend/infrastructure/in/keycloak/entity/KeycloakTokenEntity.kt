package fr.laucoin.registry.backend.infrastructure.`in`.keycloak.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The provider's token response.
 *
 * Two fields are optional because the shape here was modelled on Keycloak while the provider in use
 * is Authentik, which sends neither in the same way: it never emits `refresh_expires_in` at all, and
 * emits `refresh_token` only when `offline_access` is among the requested scopes. Declaring them
 * required made every token exchange fail to decode.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KeycloakTokenEntity(
	@field:JsonProperty("access_token")
	val accessToken: String,
	@field:JsonProperty("expires_in")
	val expiresIn: Long,
	@field:JsonProperty("refresh_expires_in")
	val refreshExpiresIn: Long? = null,
	@field:JsonProperty("refresh_token")
	val refreshToken: String? = null,
	@field:JsonProperty("token_type")
	val tokenType: String,
)
