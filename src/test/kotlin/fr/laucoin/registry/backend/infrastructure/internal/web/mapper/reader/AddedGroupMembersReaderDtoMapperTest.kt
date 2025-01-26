package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AddedGroupMembersReaderDtoMapperTest {
    private val mapper: AddedGroupMembersReaderDtoMapper = AddedGroupMembersReaderDtoMapper()

    @Test
    fun `Should toDto convert Pair of participant ids (with and without profiles) to AddedGroupMembersReaderDtoMapper`() {
        // Arrange
        val content = Pair(
            listOf(UUID.randomUUID(), UUID.randomUUID()),
            listOf(UUID.randomUUID()),
        )

        // Act
        val result = mapper.toDto(content, Locale.getDefault())

        // Assert
        assertEquals(content.first.size, result.members.size)
        assertEquals(content.second.size, result.notAddedMemberIds.size)
    }
}
