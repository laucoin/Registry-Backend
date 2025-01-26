package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.context.MessageSource

class MovementTypeDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: MovementTypeReaderDtoMapper = MovementTypeReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert Movement type as String to LabelDto`() {
        // Arrange
        val type = IN
        val translated = "translated"
        `when`(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(type, Locale.getDefault())

        // Assert
        verify(translateService, times(1)).getMessage("${MOVEMENT_TYPE_PREFIX}$type", null, Locale.getDefault())

        assertEquals(type.name, result.value)
        assertEquals(translated, result.label)
    }
}
