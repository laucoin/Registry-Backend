package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_PROFILE_STATUS_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.context.MessageSource

class EventProfileStatusReaderDtoMapperTest {
    private val translateService: MessageSource = Mockito.mock()
    private val mapper: EventProfileStatusReaderDtoMapper = EventProfileStatusReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert status as String to LabelDto`() {
        // Arrange
        val status = ACCEPTED
        val translated = "translated"
        `when`(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(status, Locale.getDefault())

        // Assert
        verify(translateService, times(1)).getMessage("${EVENT_PROFILE_STATUS_PREFIX}$status", null, Locale.getDefault())

        assertEquals(status.name, result.value)
        assertEquals(translated, result.label)
    }
}
