package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.context.MessageSource

class MovementReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val eventMapper: EventReaderDtoMapper = mock()
    private val movementContentMapper: MovementContentReaderDtoMapper = mock()
    private val mapper: MovementReaderDtoMapper = MovementReaderDtoMapper(translateService, eventMapper, movementContentMapper)

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
        val translated = "translated"
        `when`(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(movement, Locale.getDefault())

        // Assert
        verify(movementContentMapper, times(1)).toDtoList(movement.content, Locale.getDefault())
        verify(eventMapper, times(1)).toDto(movement.event !!, Locale.getDefault())

        assertEquals(movement.id, result.id)
        assertEquals(movement.dateTime, result.dateTime)
        assertEquals(translated, result.type?.label)
        assertEquals(movement.type?.name, result.type?.value)
        assertEquals(0, result.content.size)
        assertEquals(movement.visible, result.visible)
        assertEquals(movement.creation, result.creation)
        assertEquals(movement.lastEdition, result.lastEdition)
    }
}
