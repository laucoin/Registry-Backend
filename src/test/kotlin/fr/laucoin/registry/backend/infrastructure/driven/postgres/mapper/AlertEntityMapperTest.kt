package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum.HIGH
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert.AlertEntity
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class AlertEntityMapperTest {
	private val mapper = AlertEntityMapper()

	@Test
	fun `Should toModel keep the criticality and build the last communication`() {
		// Arrange
		val dateTime = ZonedDateTime.now(ZoneOffset.UTC)
		val entity = AlertEntity(
			criticality = HIGH,
			lastCommunicationDateTime = dateTime,
			lastCommunicationMessage = "message",
		)

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertEquals(HIGH, model.criticality)
		assertEquals(dateTime, model.lastCommunication?.dateTime)
		assertEquals("message", model.lastCommunication?.message)
	}

	@Test
	fun `Should toModel leave the last communication empty without communication date`() {
		// Arrange
		val entity = AlertEntity(lastCommunicationMessage = "orphan")

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertNull(model.criticality)
		assertNull(model.lastCommunication)
	}
}
