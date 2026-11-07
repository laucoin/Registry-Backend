package fr.laucoin.registry.backend.domain.service.support.impl

import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FeatureServiceTest {
	@ParameterizedTest
	@ValueSource(booleans = [true, false])
	fun `Should isInviteByEmailEnabled return the configured value`(enabled: Boolean) {
		// Arrange
		val service = FeatureService(enabled)

		// Act
		val result = service.isInviteByEmailEnabled()

		// Assert
		assertEquals(enabled, result)
	}
}
