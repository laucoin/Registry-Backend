package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.AlertQueryDto
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AlertQueryDtoMapperTest {
	private val mapper = AlertQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the alert filters, the sort and the given visibility`() {
		// Arrange
		val from = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val status = AlertStatusEnum.entries.first()
		val criticality = AlertCriticalityEnum.entries.last()
		val dto = AlertQueryDto(query = "lost", isVisible = true, status = status, criticality = criticality, from = from, sort = "-criticality")

		// Act
		val result = mapper.toSearchParams(dto, isVisible = false)

		// Assert
		assertEquals("lost", result.query)
		assertEquals(false, result.isVisible)
		assertEquals(listOf(status), result.status)
		assertEquals(criticality, result.criticality)
		assertEquals(from, result.from)
		assertNull(result.to)
		assertEquals(SortModel("criticality", true), result.sort)
	}

	@Test
	fun `Should toSearchParams keep every status when none is requested`() {
		// Act
		val result = mapper.toSearchParams(AlertQueryDto(), isVisible = null)

		// Assert
		assertEquals(AlertStatusEnum.entries.toList(), result.status)
		assertNull(result.sort)
	}
}
