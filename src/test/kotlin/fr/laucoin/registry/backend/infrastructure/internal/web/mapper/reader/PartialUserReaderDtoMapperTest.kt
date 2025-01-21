package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class PartialUserReaderDtoMapperTest {
    private val mapper: PartialUserReaderDtoMapper = PartialUserReaderDtoMapper()

    @Test
    fun `Should toDto convert UserModel to PartialUserReaderDto`() {
        // Arrange
        val user = UserModel().apply {
            id = UUID.randomUUID()
            firstName = "John"
            lastName = "DOE"
            email = "john.doe@test.com"
        }

        // Act
        val result = mapper.toDto(user, Locale.getDefault())

        // Assert
        assertEquals(user.id, result.id)
        assertEquals(user.firstName, result.firstName)
        assertEquals(user.lastName, result.lastName)
        assertEquals(user.email, result.email)
    }
}
