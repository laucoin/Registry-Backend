package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectPurgeReaderDtoMapperTest {
	@Test
	fun `Should map a content purge model to its dto`() {
		// Arrange
		val model = ProjectContentPurgeModel(listOf(UUID.randomUUID()), listOf(UUID.randomUUID()), listOf(UUID.randomUUID()))

		// Act
		val result = ProjectContentPurgeReaderDtoMapper().toDto(model)

		// Assert
		assertEquals(model.movements, result.movements)
		assertEquals(model.alerts, result.alerts)
		assertEquals(model.communications, result.communications)
	}

	@Test
	fun `Should map a configuration purge model to its dto`() {
		// Arrange
		val model = ProjectConfigurationPurgeModel(
			listOf(UUID.randomUUID()), listOf(UUID.randomUUID()), listOf(UUID.randomUUID()), listOf(UUID.randomUUID()),
		)

		// Act
		val result = ProjectConfigurationPurgeReaderDtoMapper().toDto(model)

		// Assert
		assertEquals(model.vehicles, result.vehicles)
		assertEquals(model.participants, result.participants)
		assertEquals(model.activities, result.activities)
		assertEquals(model.groups, result.groups)
	}
}
