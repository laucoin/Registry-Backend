package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.GroupReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ProjectReaderDto
import java.time.LocalDate
import java.util.Locale
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

class ParticipantReaderDtoMapperTest {
    private val partialUserMapper: PartialUserReaderDtoMapper = mock()
    private val typeMapper: ParticipantTypeReaderDtoMapper = mock()
    private val statusMapper: PresenceStatusReaderDtoMapper = mock()
    private val projectMapper: ProjectReaderDtoMapper = mock()
    private val groupMapper: GroupWithoutMemberReaderDtoMapper = mock()
    private val mapper: ParticipantReaderDtoMapper =
        ParticipantReaderDtoMapper(partialUserMapper, typeMapper, statusMapper, projectMapper, groupMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert ParticipantModel to ParticipantReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    ParticipantModel(),
                    false,
                    0,
                    0,
                ),
                Arguments.of(
                    ParticipantModel().apply { birthday = LocalDate.now() },
                    false,
                    0,
                    0,
                ),
                Arguments.of(
                    ParticipantModel().apply { birthday = LocalDate.EPOCH },
                    true,
                    0,
                    0,
                ),
                Arguments.of(
                    ParticipantModel().apply {
                        project = ProjectModel()
                        status = IN
                        user = UserModel()
                        birthday = LocalDate.now().minusYears(18)
                    },
                    true,
                    1,
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert ParticipantModel to ParticipantReaderDto`(
        participant: ParticipantModel,
        expectedMajor: Boolean,
        expectedProjectCast: Int,
        expectedUserCast: Int,
    ) {
        // Arrange
        whenever(partialUserMapper.toDto(any(), any())).thenReturn(PartialUserReaderDto())
        whenever(projectMapper.toDto(any(), any())).thenReturn(ProjectReaderDto())
        whenever(groupMapper.toDtoList(any(), any())).thenReturn(listOf(GroupReaderDto()))

        // Act
        val result = mapper.toDto(participant, Locale.getDefault())

        // Assert
        verify(partialUserMapper, times(expectedUserCast)).toDto(participant.user ?: UserModel(), Locale.getDefault())
        verify(projectMapper, times(expectedProjectCast)).toDto(participant.project ?: ProjectModel(), Locale.getDefault())
        verify(groupMapper, times(2)).toDtoList(participant.groups, Locale.getDefault())

        assertEquals(participant.id, result.id)
        assertEquals(participant.firstName, result.firstName)
        assertEquals(participant.lastName, result.lastName)
        assertEquals(participant.birthday, result.birthday)
        assertEquals(expectedMajor, result.major)
        assertEquals(participant.startAvailability, result.startAvailability)
        assertEquals(participant.endAvailability, result.endAvailability)
        assertEquals(participant.purged, result.purged)
        assertEquals(participant.visible, result.visible)
        assertEquals(participant.creation, result.creation)
        assertEquals(participant.lastEdition, result.lastEdition)
    }
}
