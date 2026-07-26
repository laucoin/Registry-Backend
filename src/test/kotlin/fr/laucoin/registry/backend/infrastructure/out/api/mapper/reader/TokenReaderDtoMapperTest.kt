package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.TokenModel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TokenReaderDtoMapperTest {
	private val mapper: TokenReaderDtoMapper = TokenReaderDtoMapper()

	@Test
	fun `Should toDto convert TokenModel to TokenReaderDto`() {
		// Arrange
		val model = TokenModel(
			accessToken = "accessToken",
			expiresIn = 3600,
			refreshExpiresIn = 18000,
			refreshToken = "refreshToken",
			tokenType = "Bearer",
		)

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(model.accessToken, result.accessToken)
		assertEquals(model.expiresIn, result.expiresIn)
		assertEquals(model.refreshExpiresIn, result.refreshExpiresIn)
		assertEquals(model.refreshToken, result.refreshToken)
		assertEquals(model.tokenType, result.tokenType)
	}
}
