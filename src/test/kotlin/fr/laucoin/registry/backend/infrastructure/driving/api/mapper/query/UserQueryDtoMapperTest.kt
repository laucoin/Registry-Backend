package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.UserQueryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class UserQueryDtoMapperTest {
	private val mapper = UserQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the filters and the sort`() {
		// Arrange
		val dto = UserQueryDto(page = 1, size = 10, query = "doe", isVisible = false, role = "USER", sort = "lastLogin")

		// Act
		val result = mapper.toSearchParams(dto)

		// Assert
		assertEquals("doe", result.query)
		assertEquals(false, result.isVisible)
		assertEquals("USER", result.role)
		assertEquals(SortModel("lastLogin", false), result.sort)
		assertEquals(PageableModel(offset = 10, limit = 10), mapper.toPageable(dto))
	}

	@Test
	fun `Should toSearchParams build empty filters when no parameter is given`() {
		// Arrange
		val dto = UserQueryDto()

		// Act
		val result = mapper.toSearchParams(dto)

		// Assert
		assertNotNull(result)
		assertNull(result.query)
		assertNull(result.isVisible)
		assertNull(result.role)
		assertNull(result.sort)
	}
}
