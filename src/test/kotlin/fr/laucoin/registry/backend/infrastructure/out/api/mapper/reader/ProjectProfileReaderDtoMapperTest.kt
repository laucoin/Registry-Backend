package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.service.ITranslateService
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProjectProfileReaderDtoMapperTest {
	private val translateService: ITranslateService = mock()
	private val projectMapper: ProjectReaderDtoMapper = mock()
	private val partialUserMapper: PartialUserReaderDtoMapper = mock()
	private val availabilityMapper: AvailabilityStatusReaderDtoMapper = mock()
	private val mapper: ProjectProfileReaderDtoMapper =
		ProjectProfileReaderDtoMapper(translateService, projectMapper, availabilityMapper, partialUserMapper)

	private companion object {
		@JvmStatic
		fun `Should toDto convert ProjectProfileModel to ProjectProfileReaderDto`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					ProjectProfileModel().apply {
						id = UUID.randomUUID()
						startAccess = CustomDateTimeModel.MIN
						endAccess = CustomDateTimeModel.MAX
						visible = false
						status = null
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					BLOCKED.name,
					0,
					0,
				),
				Arguments.of(
					ProjectProfileModel().apply {
						id = UUID.randomUUID()
						startAccess = CustomDateTimeModel.MIN
						endAccess = CustomDateTimeModel.MAX
						status = ACCEPTED
						visible = false
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					BLOCKED.name,
					0,
					0,
				),
				Arguments.of(
					ProjectProfileModel().apply {
						id = UUID.randomUUID()
						project = ProjectModel()
						user = UserModel()
						role = "ROLE"
						status = ACCEPTED
						startAccess = CustomDateTimeModel.MIN
						endAccess = CustomDateTimeModel.MAX
						visible = true
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					ACCEPTED.name,
					1,
					1,
				),
				Arguments.of(
					ProjectProfileModel().apply {
						id = UUID.randomUUID()
						project = ProjectModel()
						user = UserModel()
						role = "ROLE"
						startAccess = CustomDateTimeModel.MIN
						endAccess = CustomDateTimeModel.MAX
						status = null
						visible = true
						creation = HistoryModel()
						lastEdition = HistoryModel()
					},
					null,
					1,
					1,
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toDto convert ProjectProfileModel to ProjectProfileReaderDto`(
		profile: ProjectProfileModel,
		expectedStatusValue: String?,
		expectedProjectCast: Int,
		expectedUserCast: Int,
	) {
		// Arrange
		whenever(translateService.getMessage(any(), anyOrNull(), anyOrNull())).thenReturn("translated")
		whenever(projectMapper.toDto(any())).thenReturn(ProjectReaderDto())
		whenever(partialUserMapper.toDto(any())).thenReturn(PartialUserReaderDto())

		// Act
		val result = mapper.toDto(profile)

		// Assert
		verify(projectMapper, times(expectedProjectCast)).toDto(profile.project ?: ProjectModel())
		verify(partialUserMapper, times(expectedUserCast)).toDto(profile.user ?: UserModel())

		assertEquals(profile.id, result.id)
		assertEquals(profile.role, result.role?.value)
		assertEquals(expectedStatusValue, result.status?.value)
		assertEquals(profile.startAccess, profile.startAccess)
		assertEquals(profile.endAccess, profile.endAccess)
		assertEquals(profile.visible, result.visible)
		assertEquals(profile.creation, result.creation)
		assertEquals(profile.lastEdition, result.lastEdition)
	}
}
