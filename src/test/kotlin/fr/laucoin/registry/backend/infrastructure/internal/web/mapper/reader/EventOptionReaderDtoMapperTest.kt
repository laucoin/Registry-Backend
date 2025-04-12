package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_FORM_ASK_PREFIX
import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.ACTIVITY_COMMUNICATION
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource

class EventOptionReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: EventOptionsReaderDtoMapper = EventOptionsReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert Event option rules map to EventOptionModel List`() {
        // Arrange
        val option: Pair<EventOptionEnum, Collection<EventOptionEnum>> = Pair(ACTIVITY_COMMUNICATION, listOf(ACTIVITY))
        val label = "translated"
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(label)

        // Act
        val result = mapper.toDto(option, Locale.getDefault())

        // Assert
        verify(translateService).getMessage("${EVENT_OPTION_NAME_PREFIX}$ACTIVITY_COMMUNICATION", null, Locale.getDefault())
        verify(translateService).getMessage("${EVENT_OPTION_FORM_ASK_PREFIX}$ACTIVITY_COMMUNICATION", null, Locale.getDefault())
        verify(translateService).getMessage("${EVENT_OPTION_NAME_PREFIX}$ACTIVITY", null, Locale.getDefault())

        assertEquals(option.first, result.value)
        assertEquals(label, result.label)
        assertEquals(label, result.ask)
        assertEquals(option.second.size, result.preRequired.size)
    }
}
