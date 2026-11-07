package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuditPurgeReaderDtoMapperTest {
	private val mapper = AuditPurgeReaderDtoMapperImpl()

	@Test
	fun `Should toDto report the number of purged audit events`() {
		// Act
		val result = mapper.toDto(42L)

		// Assert
		assertEquals(42L, result.count)
	}
}
