package fr.laucoin.registry.backend.domain.validator

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class JwtIssuerOriginValidatorTest {
	private val validator = JwtIssuerOriginValidator("https://idp.example.com")

	private fun jwt(issuer: String?): Jwt {
		val builder = Jwt.withTokenValue("token")
			.header("alg", "RS256")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60))
		issuer?.let { builder.claim("iss", it) }
		return builder.build()
	}

	@Test
	fun `Should validate succeed when issuer origin matches the trusted identity provider`() {
		// Arrange
		val token = jwt("https://idp.example.com/application/o/registry/")

		// Act
		val result = validator.validate(token)

		// Assert
		assertFalse(result.hasErrors())
	}

	@Test
	fun `Should validate succeed when issuer origin matches for a different application slug`() {
		// Arrange
		val token = jwt("https://idp.example.com/application/o/registry-swagger/")

		// Act
		val result = validator.validate(token)

		// Assert
		assertFalse(result.hasErrors())
	}

	@Test
	fun `Should validate fail when issuer origin does not match the trusted identity provider`() {
		// Arrange
		val token = jwt("https://attacker.example.com/application/o/registry/")

		// Act
		val result = validator.validate(token)

		// Assert
		assertTrue(result.hasErrors())
	}

	@Test
	fun `Should validate fail when issuer claim is missing`() {
		// Arrange
		val token = jwt(null)

		// Act
		val result = validator.validate(token)

		// Assert
		assertTrue(result.hasErrors())
	}
}
