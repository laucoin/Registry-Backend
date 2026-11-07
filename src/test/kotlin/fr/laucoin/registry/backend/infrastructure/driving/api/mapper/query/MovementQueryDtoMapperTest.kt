package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.MovementQueryDto
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MovementQueryDtoMapperTest {
	private val mapper = MovementQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the movement filters, the activity, the sort and the given visibility`() {
		// Arrange
		val activityId = UUID.randomUUID()
		val from = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val dto = MovementQueryDto(
			page = 3, size = 10, hasActivity = true, isVisible = false, type = IN, from = from, activityId = activityId, sort = "-type",
		)

		// Act
		val result = mapper.toSearchParams(dto, isVisible = true)

		// Assert
		assertEquals(true, result.isVisible)
		assertEquals(true, result.hasActivity)
		assertEquals(listOf(IN), result.type)
		assertEquals(from, result.from)
		assertNull(result.to)
		assertEquals(activityId, result.activityId)
		assertEquals(SortModel("type", true), result.sort)
		assertEquals(PageableModel(offset = 30, limit = 10), mapper.toPageable(dto))
	}

	@Test
	fun `Should toSearchParams keep every movement type when none is requested`() {
		// Act
		val result = mapper.toSearchParams(MovementQueryDto(), isVisible = null)

		// Assert
		assertEquals(MovementTypeEnum.entries.toList(), result.type)
		assertNull(result.sort)
	}
}
