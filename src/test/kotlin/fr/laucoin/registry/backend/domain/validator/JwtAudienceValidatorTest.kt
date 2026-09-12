package fr.laucoin.registry.backend.domain.validator

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class JwtAudienceValidatorTest {
	private val validator = JwtAudienceValidator(setOf("registry", "registry-swagger"))

	private fun jwt(audience: List<String>): Jwt = Jwt.withTokenValue("token")
		.header("alg", "RS256")
		.claim("aud", audience)
		.issuedAt(Instant.now())
		.expiresAt(Instant.now().plusSeconds(60))
		.build()

	@Test
	fun `Should validate succeed when audience contains a trusted client id`() {
		// Arrange
		val token = jwt(listOf("registry"))

		// Act
		val result = validator.validate(token)

		// Assert
		assertFalse(result.hasErrors())
	}

	@Test
	fun `Should validate succeed when audience contains the public client id among others`() {
		// Arrange
		val token = jwt(listOf("some-other-app", "registry-swagger"))

		// Act
		val result = validator.validate(token)

		// Assert
		assertFalse(result.hasErrors())
	}

	@Test
	fun `Should validate fail when audience does not contain a trusted client id`() {
		// Arrange
		val token = jwt(listOf("another-application"))

		// Act
		val result = validator.validate(token)

		// Assert
		assertTrue(result.hasErrors())
	}

	@Test
	fun `Should validate fail when audience is empty`() {
		// Arrange
		val token = jwt(emptyList())

		// Act
		val result = validator.validate(token)

		// Assert
		assertTrue(result.hasErrors())
	}
}
