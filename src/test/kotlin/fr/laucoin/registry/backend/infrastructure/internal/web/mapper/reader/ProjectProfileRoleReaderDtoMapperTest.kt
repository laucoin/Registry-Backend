package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.PROJECT_PROFILE_ROLE_PREFIX
import java.util.Locale
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource

class ProjectProfileRoleReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: ProjectProfileRoleReaderDtoMapper = ProjectProfileRoleReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert role as String to LabelDto`() {
        // Arrange
        val role = "ROLE"
        val translated = "translated"
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn(translated)

        // Act
        val result = mapper.toDto(role, Locale.getDefault())

        // Assert
        verify(translateService).getMessage("${PROJECT_PROFILE_ROLE_PREFIX}ROLE", null, Locale.getDefault())

        assertEquals(role, result.value)
        assertEquals(translated, result.label)
    }
}
