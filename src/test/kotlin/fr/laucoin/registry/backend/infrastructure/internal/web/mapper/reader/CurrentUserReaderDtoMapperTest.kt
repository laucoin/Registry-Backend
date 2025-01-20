package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.UserPermissionConst.REGISTRY_EVENT_R
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CurrentUserReaderDtoMapperTest {
    private val mapper: CurrentUserReaderDtoMapper = CurrentUserReaderDtoMapper()

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

        // Act
        val result = mapper.toDto(currentUser)

        // Assert
        assertEquals(currentUser.id, result.id)
        assertEquals(1, result.authorities.size)
        assertEquals(authority, result.authorities.first())
        assertEquals(currentUser.preferences, result.preferences)
        assertEquals(currentUser.firstName, result.firstName)
        assertEquals(currentUser.lastName, result.lastName)
        assertEquals(currentUser.email, result.email)
        assertEquals(currentUser.role, result.role)
        assertEquals(currentUser.birthday, result.birthday)
        assertEquals(currentUser.lastLogin, result.lastLogin)
        assertEquals(currentUser.purged, result.purged)
        assertEquals(currentUser.visible, result.visible)
        assertEquals(currentUser.creation, result.creation)
        assertEquals(currentUser.lastEdition, result.lastEdition)
    }
}
