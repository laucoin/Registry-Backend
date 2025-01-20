package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock

class EventProfileReaderDtoMapperTest {
    private val partialUserMapper: PartialUserReaderDtoMapper = mock()
    private val mapper: EventProfileReaderDtoMapper = EventProfileReaderDtoMapper(partialUserMapper)

    @Test
    fun `Should toDto convert EventProfileModel to EventProfileReaderDto`() {
        // Arrange
        val profile = EventProfileModel().apply {
            id = UUID.randomUUID()
            event = EventModel()
            user = UserModel()
            role = "ROLE"
            status = ACCEPTED
            startAccess = ZonedDateTime.now()
            endAccess = ZonedDateTime.now()
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }

        // Act
        val result = mapper.toDto(profile)

        // Assert
        verify(partialUserMapper, times(1)).toDto(profile.user !!)

        assertEquals(profile.id, result.id)
        assertEquals(profile.event, result.event)
        assertEquals(profile.role, result.role)
        assertEquals(profile.status, result.status)
        assertEquals(profile.startAccess, profile.startAccess)
        assertEquals(profile.endAccess, profile.endAccess)
        assertEquals(profile.visible, result.visible)
        assertEquals(profile.creation, result.creation)
        assertEquals(profile.lastEdition, result.lastEdition)
    }
}
