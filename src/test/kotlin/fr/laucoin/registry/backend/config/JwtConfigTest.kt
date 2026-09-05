package fr.laucoin.registry.backend.config

import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames.AUD
import org.springframework.security.oauth2.jwt.JwtClaimNames.EXP
import org.springframework.security.oauth2.jwt.JwtClaimNames.ISS
import org.springframework.security.oauth2.jwt.JwtClaimNames.SUB

class JwtConfigTest {
	private companion object {
		private const val ISSUER = "https://test.oidc.com"
		private const val AUDIENCE = "client-test"
		private const val SWAGGER_AUDIENCE = "client-swagger"

		private fun jwt(
			issuer: String? = ISSUER,
			audience: List<String>? = listOf(AUDIENCE),
		): Jwt {
			val issuedAt = Instant.now()
			val builder = Jwt.withTokenValue("token")
				.header("alg", "RS256")
				.claim(SUB, "d2b0a6a4-0a5e-4f6f-9e4e-2b6a0f2e1c3d")
				.claim(EXP, issuedAt.plus(5, ChronoUnit.MINUTES))
				.issuedAt(issuedAt)
			issuer?.let { builder.claim(ISS, it) }
			audience?.let { builder.claim(AUD, it) }
			return builder.build()
		}
	}

	private val validator = JwtConfig(
		jwksUri = "$ISSUER/jwks",
		issuer = ISSUER,
		audiences = listOf(AUDIENCE, SWAGGER_AUDIENCE),
	).jwtValidator()

	@Test
	fun `Should accept a token issued for this application`() {
		assertFalse(validator.validate(jwt()).hasErrors())
	}

	@Test
	fun `Should accept a token whose audience contains this application among others`() {
		val result = validator.validate(jwt(audience = listOf("another-client", AUDIENCE)))

		assertFalse(result.hasErrors())
	}

	@Test
	fun `Should reject a token minted for another client of the same realm`() {
		val result = validator.validate(jwt(audience = listOf("another-client")))

		assertTrue(result.hasErrors())
		assertEquals("invalid_token", result.errors.first().errorCode)
	}

	@Test
	fun `Should reject a token carrying no audience`() {
		assertTrue(validator.validate(jwt(audience = null)).hasErrors())
	}

	@Test
	fun `Should reject a token issued by another provider`() {
		assertTrue(validator.validate(jwt(issuer = "https://evil.oidc.com")).hasErrors())
	}

	@Test
	fun `Should reject a token carrying no issuer`() {
		assertTrue(validator.validate(jwt(issuer = null)).hasErrors())
	}

	/**
	 * Swagger UI authenticates as its own public client, so its tokens carry a different `aud` than
	 * the one the frontend obtains through the backend's confidential client. Accepting a single
	 * audience would reject every "try it out" call.
	 */
	@Test
	fun `Should accept a token issued for the Swagger client`() {
		assertFalse(validator.validate(jwt(audience = listOf(SWAGGER_AUDIENCE))).hasErrors())
	}

	/**
	 * Authentik emits `aud` as a **plain string**, not an array — `id_token.aud = provider.client_id`
	 * in its source, confirmed by decoding a real token from the blueprinted local stack. Spring
	 * normalises a scalar claim into a single-element list, and this test pins that behaviour: were
	 * it to change, every login would break with no other test noticing.
	 */
	@Test
	fun `Should accept a token whose audience is a bare string rather than a list`() {
		val issuedAt = Instant.now()
		val jwt = Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.claim(SUB, "5c042096-a49e-4d87-bddc-26c6a4260786")
			.claim(ISS, ISSUER)
			.claim(AUD, AUDIENCE)
			.claim(EXP, issuedAt.plus(5, ChronoUnit.MINUTES))
			.issuedAt(issuedAt)
			.build()

		assertEquals(listOf(AUDIENCE), jwt.audience)
		assertFalse(validator.validate(jwt).hasErrors())
	}
}
