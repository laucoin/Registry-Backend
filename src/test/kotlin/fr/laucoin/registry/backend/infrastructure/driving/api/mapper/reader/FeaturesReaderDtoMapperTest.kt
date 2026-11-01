package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FeaturesReaderDtoMapperTest {
	private val mapper = FeaturesReaderDtoMapper()

	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `Should toDto expose the invite by email state`(enabled: Boolean) {
		// Arrange

		// Act
		val result = mapper.toDto(enabled)

		// Assert
		assertEquals(enabled, result.inviteByEmail)
	}
}
