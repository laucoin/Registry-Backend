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
		audience = AUDIENCE,
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
}
