package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.LabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.ProfileLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UserInvitationReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val statusHelper: StatusLabelMappingHelper = mock()
	private val mapper = UserInvitationReaderDtoMapperImpl(
		statusHelper,
		ProfileLabelMappingHelper(translateService),
		ProjectProfileRoleReaderDtoMapperImpl(LabelMappingHelper(translateService)),
	)

	init {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer { it.arguments[0] }
	}

	@Test
	fun `Should toDto convert profile and project summary without the user`() {
		// Arrange
		val projectId = UUID.randomUUID()
		val model = ProjectProfileModel(role = "ROLE", status = INVITED, startAccess = CustomDateTimeModel.MIN).apply {
			id = UUID.randomUUID()
			this.projectId = projectId
			project = ProjectModel(name = "Project", begin = CustomDateTimeModel.MIN, end = CustomDateTimeModel.MAX)
				.apply { id = projectId; favorite = true }
		}

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertEquals(model.id, result.id)
		assertEquals("ROLE", result.role?.value)
		assertEquals("INVITED", result.status?.value)
		assertEquals(CustomDateTimeModel.MIN, result.startAccess)
		assertEquals(projectId, result.project?.id)
		assertEquals("Project", result.project?.name)
		assertEquals(CustomDateTimeModel.MIN, result.project?.begin)
		assertEquals(CustomDateTimeModel.MAX, result.project?.end)
		assertEquals(true, result.project?.favorite)
	}

	@Test
	fun `Should toDto leave the project summary empty when the profile has none`() {
		// Arrange
		val model = ProjectProfileModel()

		// Act
		val result = mapper.toDto(model)

		// Assert
		assertNull(result.project)
	}
}
