package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.util.Locale
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class MovementParticipantsAndGroupsReaderDtoMapperTest {
    private val participantMapper: ParticipantReaderDtoMapper = mock()
    private val groupsMapper: GroupReaderDtoMapper = mock()
    private val mapper: MovementParticipantsAndGroupsReaderDtoMapper =
        MovementParticipantsAndGroupsReaderDtoMapper(participantMapper, groupsMapper)

    @Test
    fun `Should toDto convert Pair of participants and groups list to MovementParticipantsAndGroupsReaderDto`() {
        // Arrange
        val content = Pair(
            listOf(ParticipantModel()),
            listOf(GroupModel()),
        )

        // Act
        mapper.toDto(content, Locale.getDefault())

        // Assert
        verify(participantMapper, times(1)).toDtoList(content.first, Locale.getDefault())
        verify(groupsMapper, times(1)).toDtoList(content.second, Locale.getDefault())
    }
}
