package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DURATION_FORMAT_FAILED
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityMovementQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ActivityQueryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ActivityQueryDtoMapperTest {
	private val mapper = ActivityQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the activity filters with the parsed durations and the given visibility`() {
		// Arrange
		val dto = ActivityQueryDto(query = "hike", isAvailable = true, minDuration = "PT30M", maxDuration = "PT2H", participants = 4, sort = "-name")

		// Act
		val result = mapper.toSearchParams(dto, isVisible = false)

		// Assert
		assertEquals("hike", result.query)
		assertEquals(false, result.isVisible)
		assertEquals(true, result.isAvailable)
		assertEquals(30.minutes, result.minDuration)
		assertEquals(2.hours, result.maxDuration)
		assertEquals(4, result.participants)
		assertEquals(SortModel("name", true), result.sort)
	}

	@Test
	fun `Should toSearchParams refuse a duration filter that is not ISO 8601`() {
		// Act
		val result = assertThrows(RegistryException::class.java) { mapper.toSearchParams(ActivityQueryDto(minDuration = "two hours"), isVisible = null) }

		// Assert
		assertEquals(ACTIVITY_DURATION_FORMAT_FAILED, result.code)
	}

	@Test
	fun `Should toMovementSearchParams map the movement filters of an activity without any activity filter`() {
		// Act
		val result = mapper.toMovementSearchParams(ActivityMovementQueryDto(type = IN, sort = "type"), isVisible = true)

		// Assert
		assertEquals(true, result.isVisible)
		assertEquals(listOf(IN), result.type)
		assertNull(result.hasActivity)
		assertNull(result.activityId)
		assertEquals(SortModel("type", false), result.sort)
	}
}
