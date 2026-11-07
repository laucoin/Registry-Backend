package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.VehicleQueryDto
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VehicleQueryDtoMapperTest {
	private val mapper = VehicleQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the vehicle filters, the presence status, the sort and the given visibility`() {
		// Arrange
		val at = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val dto = VehicleQueryDto(query = "AB-123", status = PresenceStatusEnum.IN, at = at, brand = "Renault", model = " ", sort = "brand")

		// Act
		val result = mapper.toSearchParams(dto, isVisible = true)

		// Assert
		assertEquals("AB-123", result.query)
		assertEquals(true, result.isVisible)
		assertEquals(true, result.isPresent)
		assertEquals(at, result.at)
		assertEquals("Renault", result.brand)
		assertNull(result.model)
		assertEquals(SortModel("brand", false), result.sort)
	}

	@Test
	fun `Should toMovementSearchParams map the movement filters of a vehicle`() {
		// Act
		val result = mapper.toMovementSearchParams(ElementMovementQueryDto(hasActivity = false, type = OUT, sort = "-dateTime"), isVisible = null)

		// Assert
		assertEquals(false, result.hasActivity)
		assertEquals(listOf(OUT), result.type)
		assertEquals(SortModel("dateTime", true), result.sort)
	}
}
