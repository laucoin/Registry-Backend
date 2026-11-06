package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.AvailabilityStatusEnum.AVAILABLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.service.support.ITranslateService
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
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

class ProjectWithProfileReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val statusHelper: StatusLabelMappingHelper = mock()
	private val labels = LabelMappingHelper(translateService)
	private val mapper = ProjectWithProfileReaderDtoMapperImpl(
		statusHelper,
		ProjectOptionLabelReaderDtoMapperImpl(labels),
		ProjectProfileRoleReaderDtoMapperImpl(labels),
		ProfileLabelMappingHelper(translateService),
	)

	init {
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull(), any())).thenAnswer { it.arguments[0] }
	}

	private fun project() = ProjectModel(
		name = "Project",
		begin = CustomDateTimeModel.MIN,
		end = CustomDateTimeModel.MAX,
		status = AVAILABLE,
	).apply {
		id = UUID.randomUUID()
		counts = ProjectCountsModel(participants = 3, groups = 1, acceptedProfiles = 4, vehicles = 2)
	}

	@Test
	fun `Should toDto convert the project and the current user profile`() {
		// Arrange
		val status = LabelDto("AVAILABLE", "Available")
		val profile = ProjectProfileModel(role = "ROLE", endAccess = CustomDateTimeModel.MAX).apply { id = UUID.randomUUID() }
		val project = project().apply { this.profile = profile }
		whenever(statusHelper.projectStatus(any())).thenReturn(status)

		// Act
		val result = mapper.toDto(project)

		// Assert
		assertEquals(project.id, result.id)
		assertEquals("Project", result.name)
		assertEquals(status, result.status)
		assertEquals(3, result.counts?.participants)
		assertEquals(2, result.counts?.vehicles)
		assertEquals(profile.id, result.profile?.id)
		assertEquals("ROLE", result.profile?.role?.value)
		assertEquals(CustomDateTimeModel.MAX, result.profile?.endAccess)
	}

	@Test
	fun `Should toDto leave the profile empty when the user has none`() {
		// Arrange
		val project = project()

		// Act
		val result = mapper.toDto(project)

		// Assert
		assertNull(result.profile)
	}

	@Test
	fun `Should toDto leave the counts empty when the project has none`() {
		// Arrange
		val project = ProjectModel(name = "Project")

		// Act
		val result = mapper.toDto(project)

		// Assert
		assertNull(result.counts)
	}

	@Test
	fun `Should toDto carry the favorite flag of the project`() {
		// Arrange
		val project = ProjectModel().apply { favorite = true }

		// Act
		val result = mapper.toDto(project)

		// Assert
		assertEquals(true, result.favorite)
	}
}
