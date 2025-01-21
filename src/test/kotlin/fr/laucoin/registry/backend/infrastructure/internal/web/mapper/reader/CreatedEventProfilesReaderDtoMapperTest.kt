package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CreatedEventProfilesReaderDtoMapperTest {
    private val mapper: CreatedEventProfilesReaderDtoMapper = CreatedEventProfilesReaderDtoMapper()

    @Test
    fun `Should toDto convert Pair of user ids (with and without profiles) to CreatedEventProfilesReaderDto`() {
        // Arrange
        val content = Pair(
            listOf(UUID.randomUUID(), UUID.randomUUID()),
            listOf(UUID.randomUUID()),
        )

        // Act
        val result = mapper.toDto(content, Locale.getDefault())

        // Assert
        assertEquals(content.first.size, result.createdUserIds.size)
        assertEquals(content.second.size, result.notCreatedUserIds.size)
    }
}
