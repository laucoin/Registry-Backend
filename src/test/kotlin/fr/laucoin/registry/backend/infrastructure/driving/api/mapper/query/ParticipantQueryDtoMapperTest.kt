package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ElementMovementQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ParticipantQueryDto
import java.time.LocalDate
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ParticipantQueryDtoMapperTest {
	private val mapper = ParticipantQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map every participant filter, the sort and the given visibility`() {
		// Arrange
		val at = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val dto = ParticipantQueryDto(
			page = 1, size = 20, query = "doe", isMajor = true, type = REGISTERED, isVisible = true,
			status = PresenceStatusEnum.OUT, at = at, hasUser = false, onSite = true,
			birthdayFrom = LocalDate.of(2010, 1, 1), birthdayTo = LocalDate.of(2012, 1, 1), sort = "-lastName",
		)

		// Act
		val result = mapper.toSearchParams(dto, isVisible = false)

		// Assert
		assertEquals("doe", result.query)
		assertEquals(true, result.isMajor)
		assertEquals(REGISTERED, result.type)
		assertEquals(false, result.isVisible)
		assertEquals(PresenceStatusEnum.OUT, result.status)
		assertEquals(at, result.at)
		assertEquals(false, result.hasUser)
		assertEquals(true, result.isOnSite)
		assertEquals(LocalDate.of(2010, 1, 1), result.birthdayFrom)
		assertEquals(LocalDate.of(2012, 1, 1), result.birthdayTo)
		assertEquals(SortModel("lastName", true), result.sort)
		assertNull(result.presence)
		assertEquals(PageableModel(offset = 20, limit = 20), mapper.toPageable(dto))
	}

	@Test
	fun `Should toSearchParams drop a blank text search`() {
		// Act
		val result = mapper.toSearchParams(ParticipantQueryDto(query = " "), isVisible = null)

		// Assert
		assertNull(result.query)
	}

	@Test
	fun `Should toMovementSearchParams map the movement filters of a participant`() {
		// Arrange
		val from = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val to = ZonedDateTime.parse("2030-01-02T10:00:00Z")
		val dto = ElementMovementQueryDto(hasActivity = true, type = OUT, from = from, to = to, sort = "dateTime")

		// Act
		val result = mapper.toMovementSearchParams(dto, isVisible = true)

		// Assert
		assertEquals(true, result.isVisible)
		assertEquals(true, result.hasActivity)
		assertEquals(listOf(OUT), result.type)
		assertEquals(from, result.from)
		assertEquals(to, result.to)
		assertNull(result.activityId)
		assertEquals(SortModel("dateTime", false), result.sort)
	}

	@Test
	fun `Should toMovementSearchParams keep every movement type when none is requested`() {
		// Act
		val result = mapper.toMovementSearchParams(ElementMovementQueryDto(), isVisible = null)

		// Assert
		assertEquals(MovementTypeEnum.entries.toList(), result.type)
	}
}
