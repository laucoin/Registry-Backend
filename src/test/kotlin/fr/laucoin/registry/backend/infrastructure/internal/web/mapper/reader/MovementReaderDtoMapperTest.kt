package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class MovementReaderDtoMapperTest {
    private val movementContentMapper: MovementContentReaderDtoMapper = mock()
    private val mapper: MovementReaderDtoMapper = MovementReaderDtoMapper(movementContentMapper)

    @Test
    fun `Should toDto convert MovementModel to MovementReaderDto`() {
        // Arrange
        val movement = MovementModel().apply {
            id = UUID.randomUUID()
            event = EventModel()
            dateTime = ZonedDateTime.now()
            type = IN
            content = emptyList()
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }

        // Act
        val result = mapper.toDto(movement)

        // Assert
        verify(movementContentMapper, times(1)).toDtoList(movement.content)

        assertEquals(movement.id, result.id)
        assertEquals(movement.event, result.event)
        assertEquals(movement.dateTime, result.dateTime)
        assertEquals(movement.type, result.type)
        assertEquals(0, result.content.size)
        assertEquals(movement.visible, result.visible)
        assertEquals(movement.creation, result.creation)
        assertEquals(movement.lastEdition, result.lastEdition)
    }
}
