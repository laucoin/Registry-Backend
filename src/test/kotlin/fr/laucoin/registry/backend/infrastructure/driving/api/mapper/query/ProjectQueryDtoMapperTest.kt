package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectQueryDto
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProjectQueryDtoMapperTest {
	private val mapper = ProjectQueryDtoMapperImpl()

	@Test
	fun `Should toSearchParams map the filters and the sort and leave the viewer unset`() {
		// Arrange
		val at = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val dto = ProjectQueryDto(page = 2, size = 5, query = "camp", isVisible = false, isFavorite = true, at = at, sort = "-name")

		// Act
		val result = mapper.toSearchParams(dto)

		// Assert
		assertEquals("camp", result.query)
		assertEquals(false, result.isVisible)
		assertEquals(true, result.isFavorite)
		assertEquals(at, result.at)
		assertEquals(SortModel("name", true), result.sort)
		assertNull(result.userId)
		assertTrue(result.hiddenReadableIds.isEmpty())
		assertEquals(PageableModel(offset = 10, limit = 5), mapper.toPageable(dto))
	}

	@Test
	fun `Should toSearchParams drop a blank text search`() {
		// Act
		val result = mapper.toSearchParams(ProjectQueryDto(query = "  "))

		// Assert
		assertNull(result.query)
		assertNull(result.sort)
	}

	@Test
	fun `Should toSearchParams refuse a sort field outside the project allow-list`() {
		// Act
		val result = assertThrows(RegistryException::class.java) { mapper.toSearchParams(ProjectQueryDto(sort = "email")) }

		// Assert
		assertEquals(SORT_FIELD_NOT_SUPPORTED, result.code)
	}
}
