package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectReaderDto
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class ProjectWithProfileReaderDtoMapperTest {
	private val projectMapper: ProjectReaderDtoMapper = mock()
	private val profileMapper: ProjectProfileReaderDtoMapper = mock()
	private val mapper = ProjectWithProfileReaderDtoMapper(projectMapper, profileMapper)

	private val projectDto = ProjectReaderDto(
		name = "Project",
		status = LabelDto("AVAILABLE", "Available"),
		begin = CustomDateTimeModel.MIN,
		end = CustomDateTimeModel.MAX,
	).apply { id = UUID.randomUUID() }

	@Test
	fun `Should toDto convert the project and the current user profile`() {
		// Arrange
		val role = LabelDto("ROLE", "Role")
		val profile = ProjectProfileModel().apply { id = UUID.randomUUID() }
		whenever(projectMapper.toDto(any())).thenReturn(projectDto)
		whenever(profileMapper.toDto(any())).thenReturn(
			ProjectProfileReaderDto(role = role, endAccess = CustomDateTimeModel.MAX).apply { id = profile.id }
		)

		// Act
		val result = mapper.toDto(ProjectModel().apply { this.profile = profile })

		// Assert
		assertEquals(projectDto.id, result.id)
		assertEquals("Project", result.name)
		assertEquals(projectDto.status, result.status)
		assertEquals(profile.id, result.profile?.id)
		assertEquals(role, result.profile?.role)
		assertEquals(CustomDateTimeModel.MAX, result.profile?.endAccess)
	}

	@Test
	fun `Should toDto leave the profile empty when the user has none`() {
		// Arrange
		whenever(projectMapper.toDto(any())).thenReturn(projectDto)

		// Act
		val result = mapper.toDto(ProjectModel())

		// Assert
		assertNull(result.profile)
		verifyNoInteractions(profileMapper)
	}
}
