package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CustomDateTimeWriterDtoMapperTest {
	private val mapper = CustomDateTimeWriterDtoMapperImpl()

	@Test
	fun `Should map the date and the time`() {
		// Arrange
		val time = OffsetTime.of(9, 30, 0, 0, ZoneOffset.UTC)
		val dto = CustomDateTimeWriterDto(LocalDate.of(2026, 7, 1), time)

		// Act
		val result = mapper.toModel(dto)

		// Assert
		assertEquals(LocalDate.of(2026, 7, 1), result?.date)
		assertEquals(time, result?.time)
	}

	@Test
	fun `Should map a date without time`() {
		// Arrange
		val dto = CustomDateTimeWriterDto(LocalDate.of(2026, 7, 1), null)

		// Act
		val result = mapper.toModel(dto)

		// Assert
		assertEquals(LocalDate.of(2026, 7, 1), result?.date)
		assertNull(result?.time)
	}

	@Test
	fun `Should return nothing when the date is missing`() {
		// Arrange
		val dto = CustomDateTimeWriterDto(null, OffsetTime.of(9, 30, 0, 0, ZoneOffset.UTC))

		// Act
		val result = mapper.toModel(dto)

		// Assert
		assertNull(result)
	}
}
