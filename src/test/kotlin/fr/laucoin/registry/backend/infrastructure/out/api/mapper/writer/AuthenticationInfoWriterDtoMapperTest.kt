package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.AuthenticationInfoWriterDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthenticationInfoWriterDtoMapperTest {
	private val mapper: AuthenticationInfoWriterDtoMapper = AuthenticationInfoWriterDtoMapper()

	@Test
	fun `Should toModel convert AuthenticationInfoWriterDto to AuthenticationInfoModel`() {
		// Arrange
		val dto = AuthenticationInfoWriterDto(
			redirectUri = "https://app.test/callback",
			authorizationCode = "code",
		)

		// Act
		val result = mapper.toModel(dto)

		// Assert
		assertEquals(dto.redirectUri, result.redirectUri)
		assertEquals(dto.authorizationCode, result.authorizationCode)
	}
}
