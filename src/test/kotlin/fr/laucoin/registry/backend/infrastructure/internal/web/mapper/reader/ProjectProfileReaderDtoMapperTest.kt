package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import java.time.LocalDateTime
import java.util.Locale
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
import org.springframework.context.MessageSource

class ProjectProfileReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val projectMapper: ProjectReaderDtoMapper = mock()
    private val partialUserMapper: PartialUserReaderDtoMapper = mock()
    private val mapper: ProjectProfileReaderDtoMapper =
        ProjectProfileReaderDtoMapper(translateService, projectMapper, partialUserMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert ProjectProfileModel to ProjectProfileReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    ProjectProfileModel().apply {
                        id = UUID.randomUUID()
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = false
                        status = null
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    BLOCKED.name,
                    1,
                    0,
                    0,
                ),
                Arguments.of(
                    ProjectProfileModel().apply {
                        id = UUID.randomUUID()
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        status = ACCEPTED
                        visible = false
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    BLOCKED.name,
                    1,
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
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    ACCEPTED.name,
                    1,
                    1,
                    1,
                ),
                Arguments.of(
                    ProjectProfileModel().apply {
                        id = UUID.randomUUID()
                        project = ProjectModel()
                        user = UserModel()
                        role = "ROLE"
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        status = null
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    null,
                    1,
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
        expectedTranslation: Int,
        expectedProjectCast: Int,
        expectedUserCast: Int,
    ) {
        // Arrange
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn("translated")
        whenever(projectMapper.toDto(any(), any())).thenReturn(ProjectReaderDto())
        whenever(partialUserMapper.toDto(any(), any())).thenReturn(PartialUserReaderDto())

        // Act
        val result = mapper.toDto(profile, Locale.getDefault())

        // Assert
        verify(projectMapper, times(expectedProjectCast)).toDto(profile.project ?: ProjectModel(), Locale.getDefault())
        verify(partialUserMapper, times(expectedUserCast)).toDto(profile.user ?: UserModel(), Locale.getDefault())

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
