package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AuthenticationUriModel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AuthenticationUriReaderDtoMapperTest {
	private val mapper: AuthenticationUriReaderDtoMapper = AuthenticationUriReaderDtoMapper()

	@Test
	fun `Should toDto convert AuthenticationUriModel to AuthenticationUriReaderDto`() {
		// Arrange
		val model = AuthenticationUriModel(uri = "https://auth.test/login")

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(model.uri, result.uri)
	}
}
