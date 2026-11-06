package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.query

import fr.laucoin.registry.backend.domain.constant.ErrorConst.SORT_FIELD_NOT_SUPPORTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.SortModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.ProjectProfileQueryDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.query.UserProjectProfileQueryDto
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ProjectProfileQueryDtoMapperTest {
	private val mapper = ProjectProfileQueryDtoMapperImpl()

	@Test
	fun `Should toPageable turn the page number into an offset`() {
		// Arrange
		val dto = ProjectProfileQueryDto(page = 3, size = 20)

		// Act
		val result = mapper.toPageable(dto)

		// Assert
		assertEquals(PageableModel(offset = 60, limit = 20), result)
	}

	@Test
	fun `Should toSearchParams map every filter, the sort and the given visibility`() {
		// Arrange
		val at = ZonedDateTime.parse("2030-01-01T10:00:00Z")
		val dto = ProjectProfileQueryDto(
			query = "john", isAvailable = true, status = ACCEPTED, isVisible = false, at = at, role = "PROJECT_ROLE", sort = "-email",
		)

		// Act
		val result = mapper.toSearchParams(dto, isVisible = true)

		// Assert
		assertEquals("john", result.query)
		assertEquals(true, result.isAvailable)
		assertEquals(listOf(ACCEPTED), result.status)
		assertEquals(true, result.isVisible)
		assertEquals(at, result.at)
		assertEquals("PROJECT_ROLE", result.role)
		assertEquals(SortModel("email", true), result.sort)
	}

	@Test
	fun `Should toSearchParams read the blocked status as a visibility filter`() {
		// Arrange
		val dto = ProjectProfileQueryDto(status = BLOCKED)

		// Act
		val result = mapper.toSearchParams(dto, isVisible = null)

		// Assert
		assertEquals(false, result.isVisible)
		assertEquals(ProfileStatusEnum.entries.toList(), result.status)
	}

	@Test
	fun `Should toSearchParams drop a blank text search and a blank role`() {
		// Arrange
		val dto = ProjectProfileQueryDto(query = "  ", role = "")

		// Act
		val result = mapper.toSearchParams(dto, isVisible = null)

		// Assert
		assertNull(result.query)
		assertNull(result.role)
		assertNull(result.sort)
	}

	@Test
	fun `Should toSearchParams refuse a sort field outside the profile allow-list`() {
		// Arrange
		val dto = ProjectProfileQueryDto(sort = "projectName")

		// Act
		val result = assertThrows(RegistryException::class.java) { mapper.toSearchParams(dto, isVisible = null) }

		// Assert
		assertEquals(SORT_FIELD_NOT_SUPPORTED, result.code)
	}

	@Test
	fun `Should toSearchParams of the current user profiles sort by project and keep every visibility`() {
		// Arrange
		val dto = UserProjectProfileQueryDto(query = "camp", isAvailable = false, sort = "projectName")

		// Act
		val result = mapper.toSearchParams(dto)

		// Assert
		assertEquals("camp", result.query)
		assertEquals(false, result.isAvailable)
		assertNull(result.isVisible)
		assertEquals(SortModel("projectName", false), result.sort)
	}
}
