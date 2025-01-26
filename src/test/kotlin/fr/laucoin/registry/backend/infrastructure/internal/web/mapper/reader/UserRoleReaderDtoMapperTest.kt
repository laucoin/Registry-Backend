package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
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

class UserRoleReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: UserRoleReaderDtoMapper = UserRoleReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert User role as String to LabelDto`() {
        // Arrange
        val role = "ROLE"
        val translated = "translated"
        `when`(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(role, Locale.getDefault())

        // Assert
        verify(translateService, times(1)).getMessage("${USER_ROLE_PREFIX}ROLE", null, Locale.getDefault())

        assertEquals(role, result.value)
        assertEquals(translated, result.label)
    }
}
