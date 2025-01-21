package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock

class GroupWithoutMemberReaderDtoMapperTest {
    private val eventMapper: EventReaderDtoMapper = mock()
    private val mapper: GroupWithoutMemberReaderDtoMapper = GroupWithoutMemberReaderDtoMapper(eventMapper)

    @Test
    fun `Should toDto convert GroupModel to GroupReaderDto`() {
        // Arrange
        val group = GroupModel().apply {
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
        verify(eventMapper, times(1)).toDto(group.event !!, Locale.getDefault())

        assertEquals(group.id, result.id)
        assertEquals(group.name, result.name)
        assertEquals(group.begin, result.begin)
        assertEquals(group.end, result.end)
        assertEquals(group.visible, result.visible)
        assertEquals(group.creation, result.creation)
        assertEquals(group.lastEdition, result.lastEdition)
    }
}
