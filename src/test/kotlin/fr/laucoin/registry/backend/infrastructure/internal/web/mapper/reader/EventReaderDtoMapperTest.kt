package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.context.MessageSource

class EventReaderDtoMapperTest {
    private val translateService: MessageSource = Mockito.mock()
    private val mapper: EventReaderDtoMapper = EventReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert EventModel to EventReaderDto`() {
        // Arrange
        val event = EventModel().apply {
            id = UUID.randomUUID()
            name = "Name"
            begin = ZonedDateTime.now()
            end = ZonedDateTime.now()
            options = listOf(VEHICLE)
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }
        val translated = "translated"
        `when`(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(event, Locale.getDefault())

        // Assert
        verify(translateService, times(1)).getMessage("${EVENT_OPTION_NAME_PREFIX}VEHICLE", null, Locale.getDefault())

        assertEquals(event.id, result.id)
        assertEquals(event.name, result.name)
        assertEquals(event.begin, result.begin)
        assertEquals(event.end, result.end)
        assertEquals(translated, result.options?.first()?.label)
        assertEquals(event.options?.first()?.name, result.options?.first()?.value)
        assertEquals(event.visible, result.visible)
        assertEquals(event.creation, result.creation)
        assertEquals(event.lastEdition, result.lastEdition)
    }
}
