package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum.IN_PROGRESS
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertCreationWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.IdReferenceDtoMapperImpl
import fr.laucoin.registry.backend.test.ModelExt.defaultProjectId
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AlertCreationWriterDtoMapperTest {
	private val mapper = AlertCreationWriterDtoMapperImpl(IdReferenceDtoMapperImpl())
	private val dateTime = ZonedDateTime.parse("2026-07-01T09:00:00Z")

	@Test
	fun `Should create an in progress alert with its first communication`() {
		// Arrange
		val movementId = UUID.randomUUID()
		val dto = AlertCreationWriterDto(title = "Lost", dateTime = dateTime, message = "Help", movementId = movementId)

		// Act
		val result = mapper.toModel(dto, defaultProjectId)

		// Assert
		assertEquals("Lost", result.title)
		assertEquals(dateTime, result.dateTime)
		assertEquals(IN_PROGRESS, result.status)
		assertEquals(defaultProjectId, result.projectId)
		val communication = result.communications!!.single()
		assertEquals(dateTime, communication.dateTime)
		assertEquals("Help", communication.message)
		assertEquals(movementId, communication.movement?.id)
		assertEquals(defaultProjectId, communication.projectId)
	}

	@Test
	fun `Should leave the movement of the communication empty when none is given`() {
		// Arrange
		val dto = AlertCreationWriterDto(title = "Lost", dateTime = dateTime, message = "Help", movementId = null)

		// Act
		val result = mapper.toModel(dto, defaultProjectId)

		// Assert
		assertNull(result.communications!!.single().movement)
	}
}
