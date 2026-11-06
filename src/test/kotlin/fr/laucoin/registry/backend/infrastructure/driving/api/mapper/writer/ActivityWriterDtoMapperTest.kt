package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ActivityWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.NumericRangeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ActivityDurationMappingHelper
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import java.time.LocalDate
import kotlin.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST

class ActivityWriterDtoMapperTest {
	private val mapper = ActivityWriterDtoMapperImpl(
		NumericRangeWriterDtoMapperImpl(),
		CustomDateTimeWriterDtoMapperImpl(),
		ActivityDurationMappingHelper(),
	)

	@Test
	fun `Should map the fields, the duration, the range, the dates and the project`() {
		// Arrange
		val dto = ActivityWriterDto(
			name = "Hike",
			description = "On the hill",
			duration = "PT1H30M",
			allowedParticipants = NumericRangeWriterDto(2, 8),
			startAvailability = CustomDateTimeWriterDto(LocalDate.of(2026, 7, 1), null),
			endAvailability = CustomDateTimeWriterDto(LocalDate.of(2026, 7, 2), null),
		)

		// Act
		val result = mapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals("Hike", result.name)
		assertEquals("On the hill", result.description)
		assertEquals(Duration.parse("PT1H30M"), result.duration)
		assertEquals(2, result.allowedParticipants?.lower)
		assertEquals(8, result.allowedParticipants?.upper)
		assertEquals(LocalDate.of(2026, 7, 1), result.startAvailability?.date)
		assertEquals(LocalDate.of(2026, 7, 2), result.endAvailability?.date)
		assertEquals(defaultProjectId, result.projectId)
	}

	@Test
	fun `Should keep the optional fields empty when they are missing`() {
		// Arrange
		val dto = ActivityWriterDto(name = "Hike")

		// Act
		val result = mapper.toModel(dto, defaultProjectId)

		// Assert
		assertNull(result.duration)
		assertNull(result.allowedParticipants)
		assertNull(result.startAvailability)
		assertNull(result.endAvailability)
	}

	@Test
	fun `Should refuse an invalid duration with a bad request`() {
		// Arrange
		val dto = ActivityWriterDto(name = "Hike", duration = "not a duration")

		// Act
		val result = assertThrows(RegistryException::class.java) { mapper.toModel(dto, defaultProjectId) }

		// Assert
		assertEquals(BAD_REQUEST, result.status)
		assertEquals(ACTIVITY_DURATION_FORMAT_FAILED, result.message)
	}
}
