package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.GroupQueryDto
import java.time.ZonedDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class GroupQueryDtoMapperTest {
	private val mapper = GroupQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the group filters, the visibility and the member user`() {
		// Arrange
		val at = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val memberUserId = UUID.randomUUID()
		val dto = GroupQueryDto(query = "red", isPresent = true, at = at, hasOnSiteMembers = false, sort = "name")

		// Act
		val result = mapper.toSearchParams(dto, isVisible = true, memberUserId = memberUserId)

		// Assert
		assertEquals("red", result.query)
		assertEquals(true, result.isVisible)
		assertEquals(true, result.isPresent)
		assertEquals(at, result.at)
		assertEquals(memberUserId, result.memberUserId)
		assertEquals(false, result.hasOnSiteMembers)
		assertEquals(SortModel("name", false), result.sort)
	}

	@Test
	fun `Should toSearchParams leave the member user unset when the groups of the user are not requested`() {
		// Act
		val result = mapper.toSearchParams(GroupQueryDto(query = ""), isVisible = null, memberUserId = null)

		// Assert
		assertNull(result.memberUserId)
		assertNull(result.query)
	}
}
