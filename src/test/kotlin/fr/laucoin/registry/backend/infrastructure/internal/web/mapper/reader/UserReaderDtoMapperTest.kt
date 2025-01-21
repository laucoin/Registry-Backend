package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.context.MessageSource

class UserReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: UserReaderDtoMapper = UserReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert UserModel to UserReaderDto`() {
        // Arrange
        val user = UserModel().apply {
            id = UUID.randomUUID()
            firstName = "John"
            lastName = "DOE"
            email = "john.doe@test.com"
            role = "ADMIN"
            birthday = LocalDate.now()
            lastLogin = ZonedDateTime.now()
            purged = false
        }
        val expectedRole = "Administrator"
        `when`(translateService.getMessage("${USER_ROLE_PREFIX}ADMIN", null, Locale.getDefault())).thenReturn(expectedRole)

        // Act
        val result = mapper.toDto(user, Locale.getDefault())

        // Assert
        verify(translateService, times(1)).getMessage("${USER_ROLE_PREFIX}ADMIN", null, Locale.getDefault())

        assertEquals(user.id, result.id)
        assertEquals(user.firstName, result.firstName)
        assertEquals(user.lastName, result.lastName)
        assertEquals(user.email, result.email)
        assertEquals(expectedRole, result.role?.label)
        assertEquals(user.role, result.role?.value)
        assertEquals(user.birthday, result.birthday)
        assertEquals(user.lastLogin, result.lastLogin)
        assertEquals(user.purged, result.purged)
    }
}
