package fr.laucoin.registry.backend.infrastructure.out.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.out.api.dto.writer.RefreshAuthenticationInfoWriterDto
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class RefreshAuthenticationInfoWriterDtoMapperTest {
	private val mapper: RefreshAuthenticationInfoWriterDtoMapper = RefreshAuthenticationInfoWriterDtoMapper()

	@Test
	fun `Should toModel convert RefreshAuthenticationInfoWriterDto to RefreshAuthenticationInfoModel`() {
		// Arrange
		val dto = RefreshAuthenticationInfoWriterDto(refreshToken = "refreshToken")

		// Act
		val result = mapper.toModel(dto)

		// Assert
		assertEquals(dto.refreshToken, result.refreshToken)
	}
}
