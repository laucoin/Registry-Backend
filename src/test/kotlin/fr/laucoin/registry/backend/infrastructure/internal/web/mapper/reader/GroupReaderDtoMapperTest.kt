package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class GroupReaderDtoMapperTest {
    private val eventMapper: EventReaderDtoMapper = mock()
    private val participantMapper: ParticipantReaderDtoMapper = mock()
    private val mapper: GroupReaderDtoMapper = GroupReaderDtoMapper(eventMapper, participantMapper)

    @Test
    fun `Should toDto convert GroupModel to GroupAndContentReaderDto`() {
        // Arrange
        val group = GroupModel(
            members = emptyList(),
        ).apply {
            id = UUID.randomUUID()
            event = EventModel()
            name = "Event"
            begin = ZonedDateTime.now()
            end = ZonedDateTime.now()
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }

        // Act
        val result = mapper.toDto(group, Locale.getDefault())

        // Assert
        verify(participantMapper, times(1)).toDtoList(group.members, Locale.getDefault())
        verify(eventMapper, times(1)).toDto(group.event !!, Locale.getDefault())

        assertEquals(group.id, result.id)
        assertEquals(0, result.members.size)
        assertEquals(group.name, result.name)
        assertEquals(group.begin, result.begin)
        assertEquals(group.end, result.end)
        assertEquals(group.visible, result.visible)
        assertEquals(group.creation, result.creation)
        assertEquals(group.lastEdition, result.lastEdition)
    }
}
