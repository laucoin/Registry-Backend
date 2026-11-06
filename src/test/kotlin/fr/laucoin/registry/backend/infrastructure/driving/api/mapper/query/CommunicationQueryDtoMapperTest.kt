package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.CommunicationQueryDto
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CommunicationQueryDtoMapperTest {
	private val mapper = CommunicationQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the text search, the period, the sort and the given visibility`() {
		// Arrange
		val from = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val to = ZonedDateTime.parse("2030-01-02T10:00:00Z")
		val dto = CommunicationQueryDto(query = "call", isVisible = true, from = from, to = to, sort = "-dateTime")

		// Act
		val result = mapper.toSearchParams(dto, isVisible = false)

		// Assert
		assertEquals("call", result.query)
		assertEquals(false, result.isVisible)
		assertEquals(from, result.from)
		assertEquals(to, result.to)
		assertEquals(SortModel("dateTime", true), result.sort)
	}

	@Test
	fun `Should toSearchParams drop a blank text search`() {
		// Act
		val result = mapper.toSearchParams(CommunicationQueryDto(query = "  "), isVisible = null)

		// Assert
		assertNull(result.query)
	}
}
