package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.USER_ROLE_PREFIX
import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.context.MessageSource
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CurrentUserReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: CurrentUserReaderDtoMapper = CurrentUserReaderDtoMapper(translateService)

    @Test
    fun `Should toDto convert CurrentUserModel to CurrentUserReaderDto`() {
        // Arrange
        val authority = REGISTRY_EVENT_R
        val currentUser = CurrentUserModel(
            authorities = mutableListOf(SimpleGrantedAuthority(authority)),
            preferences = PreferencesModel(),
        ).apply {
            id = UUID.randomUUID()
            oidcId = UUID.randomUUID()
            firstName = "John"
            lastName = "DOE"
            email = "john.doe@test.com"
            role = "ROLE"
            birthday = LocalDate.now()
            lastLogin = ZonedDateTime.now()
            purged = false
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }
        val label = "Role translated"
        `when`(translateService.getMessage("${USER_ROLE_PREFIX}ROLE", null, Locale.getDefault())).thenReturn(label)

        // Act
        val result = mapper.toDto(currentUser, Locale.getDefault())

        // Assert
        assertEquals(currentUser.id, result.id)
        assertEquals(1, result.authorities.size)
        assertEquals(authority, result.authorities.first())
        assertEquals(currentUser.preferences, result.preferences)
        assertEquals(currentUser.firstName, result.firstName)
        assertEquals(currentUser.lastName, result.lastName)
        assertEquals(currentUser.email, result.email)
        assertEquals(label, result.role?.label)
        assertEquals(currentUser.role, result.role?.value)
        assertEquals(currentUser.birthday, result.birthday)
        assertEquals(currentUser.lastLogin, result.lastLogin)
        assertEquals(currentUser.purged, result.purged)
        assertEquals(currentUser.visible, result.visible)
        assertEquals(currentUser.creation, result.creation)
        assertEquals(currentUser.lastEdition, result.lastEdition)
    }
}
