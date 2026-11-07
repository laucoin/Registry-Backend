package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.VehicleWriterDto
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VehicleWriterDtoMapperTest {
	private val mapper = VehicleWriterDtoMapperImpl(CustomDateTimeWriterDtoMapperImpl())

	@Test
	fun `Should upper case the license plate and map the other fields`() {
		// Arrange
		val dto = VehicleWriterDto(
			licensePlate = "ab-123-cd",
			brand = "Renault",
			model = "Trafic",
			startAvailability = CustomDateTimeWriterDto(LocalDate.of(2026, 7, 1), null),
		)

		// Act
		val result = mapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals("AB-123-CD", result.licensePlate)
		assertEquals("Renault", result.brand)
		assertEquals("Trafic", result.model)
		assertEquals(LocalDate.of(2026, 7, 1), result.startAvailability?.date)
		assertNull(result.endAvailability)
		assertEquals(defaultProjectId, result.projectId)
	}

	@Test
	fun `Should keep a missing license plate empty`() {
		// Arrange
		val dto = VehicleWriterDto(licensePlate = null)

		// Act
		val result = mapper.toModel(dto, defaultProjectId)

		// Assert
		assertNull(result.licensePlate)
	}
}
