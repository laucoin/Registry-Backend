package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum.SHOPPING
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEventEntity
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class MovementEventEntityMapperTest {
	private val mapper = MovementEventEntityMapper()
	private val dateTime = ZonedDateTime.of(2026, 7, 1, 9, 0, 0, 0, UTC)

	@Test
	fun `Should map the date, the type and the reason`() {
		// Arrange
		val entity = MovementEventEntity(dateTime, OUT, SHOPPING)

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertEquals(dateTime, model.dateTime)
		assertEquals(OUT, model.type)
		assertEquals(SHOPPING, model.reason)
	}

	@Test
	fun `Should keep a missing reason empty`() {
		// Arrange
		val entity = MovementEventEntity(dateTime, OUT, null)

		// Act
		val model = mapper.toModel(entity)

		// Assert
		assertNull(model.reason)
	}
}
