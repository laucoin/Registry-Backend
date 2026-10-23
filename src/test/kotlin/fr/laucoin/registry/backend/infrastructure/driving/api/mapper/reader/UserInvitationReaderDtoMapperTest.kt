package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectProfileReaderDto
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserInvitationReaderDtoMapperTest {
	private val profileMapper: ProjectProfileReaderDtoMapper = mock()
	private val mapper = UserInvitationReaderDtoMapper(profileMapper)

	private val role = LabelDto("ROLE", "Role")
	private val status = LabelDto("INVITED", "Invited")

	@Test
	fun `Should toDto convert profile and project summary without the user`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val model = ProjectProfileModel().apply {
			id = UUID.randomUUID()
			this.projectId = projectId
			project = ProjectModel(name = "Project", begin = CustomDateTimeModel.MIN, end = CustomDateTimeModel.MAX)
				.apply { id = projectId }
		}
		whenever(profileMapper.toDto(any())).thenReturn(
			ProjectProfileReaderDto(role = role, status = status, startAccess = CustomDateTimeModel.MIN).apply {
				id = model.id
				this.projectId = projectId
			}
		)

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(model.id, result.id)
		assertEquals(projectId, result.projectId)
		assertEquals(role, result.role)
		assertEquals(status, result.status)
		assertEquals(CustomDateTimeModel.MIN, result.startAccess)
		assertEquals(projectId, result.project?.id)
		assertEquals("Project", result.project?.name)
		assertEquals(CustomDateTimeModel.MIN, result.project?.begin)
		assertEquals(CustomDateTimeModel.MAX, result.project?.end)
	}

	@Test
	fun `Should toDto leave the project summary empty when the profile has none`() {
		// Arrange
		whenever(profileMapper.toDto(any())).thenReturn(ProjectProfileReaderDto())

		// Act
		val result = mapper.toDto(ProjectProfileModel())

		// Assert
		assertNull(result.project)
	}
}
