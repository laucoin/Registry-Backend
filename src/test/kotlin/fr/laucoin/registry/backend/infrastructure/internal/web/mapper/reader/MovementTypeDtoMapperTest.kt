package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource

class MovementTypeDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: MovementTypeReaderDtoMapper = MovementTypeReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert Movement type as String to LabelDto`() {
        // Arrange
        val type = IN
        val translated = "translated"
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(type, Locale.getDefault())

        // Assert
        verify(translateService).getMessage("${MOVEMENT_TYPE_PREFIX}$type", null, Locale.getDefault())

        assertEquals(type.name, result.value)
        assertEquals(translated, result.label)
    }
}
