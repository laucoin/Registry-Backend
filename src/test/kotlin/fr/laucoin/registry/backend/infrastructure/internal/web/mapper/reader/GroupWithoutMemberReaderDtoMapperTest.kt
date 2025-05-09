package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectReaderDto
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GroupWithoutMemberReaderDtoMapperTest {
    private val projectMapper: ProjectReaderDtoMapper = mock()
    private val mapper: GroupWithoutMemberReaderDtoMapper = GroupWithoutMemberReaderDtoMapper(projectMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert GroupModel to GroupAndContentReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    GroupModel(
                        members = emptyList(),
                    ).apply {
                        id = UUID.randomUUID()
                        name = "Project"
                        startAvailability = CustomDateTimeModel.MIN
                        startAvailability = CustomDateTimeModel.MAX
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    0,
                ),
                Arguments.of(
                    GroupModel(
                        members = emptyList(),
                    ).apply {
                        id = UUID.randomUUID()
                        project = ProjectModel()
                        name = "Project"
                        startAvailability = CustomDateTimeModel.MIN
                        startAvailability = CustomDateTimeModel.MAX
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert GroupModel to GroupAndContentReaderDto`(
        group: GroupModel,
        expectedProjectCast: Int,
    ) {
        // Arrange
        whenever(projectMapper.toDto(any(), any())).thenReturn(ProjectReaderDto())

        // Act
        val result = mapper.toDto(group, Locale.getDefault())

        // Assert
        verify(projectMapper, times(expectedProjectCast)).toDto(group.project ?: ProjectModel(), Locale.getDefault())

        assertEquals(group.id, result.id)
        assertEquals(group.name, result.name)
        assertEquals(group.startAvailability, result.startAvailability)
        assertEquals(group.endAvailability, result.endAvailability)
        assertEquals(group.visible, result.visible)
        assertEquals(group.creation, result.creation)
        assertEquals(group.lastEdition, result.lastEdition)
    }
}
