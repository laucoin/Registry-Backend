package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock

class ParticipantReaderDtoMapperTest {
    private val partialUserMapper: PartialUserReaderDtoMapper = mock()
    private val eventMapper: EventReaderDtoMapper = Mockito.mock()
    private val groupMapper: GroupWithoutMemberReaderDtoMapper = mock()
    private val mapper: ParticipantReaderDtoMapper = ParticipantReaderDtoMapper(partialUserMapper, eventMapper, groupMapper)

    @Test
    fun `Should toDto convert ParticipantModel to ParticipantReaderDto`() {
        // Arrange
        val participant = ParticipantModel().apply {
            id = UUID.randomUUID()
            event = EventModel()
            firstName = "John"
            lastName = "DOE"
            birthday = LocalDate.now()
            groups = emptyList()
            user = UserModel()
            begin = ZonedDateTime.now()
            end = ZonedDateTime.now()
            purged = false
            visible = true
            creation = HistoryModel()
            lastEdition = HistoryModel()
        }

        // Act
        val result = mapper.toDto(participant, Locale.getDefault())

        // Assert
        verify(partialUserMapper, times(1)).toDto(participant.user !!, Locale.getDefault())
        verify(groupMapper, times(1)).toDtoList(participant.groups, Locale.getDefault())
        verify(eventMapper, times(1)).toDto(participant.event !!, Locale.getDefault())

        assertEquals(participant.id, result.id)
        assertEquals(participant.firstName, result.firstName)
        assertEquals(participant.lastName, result.lastName)
        assertEquals(participant.birthday, result.birthday)
        assertEquals(false, result.major)
        assertEquals(0, result.groups.size)
        assertEquals(participant.begin, result.begin)
        assertEquals(participant.end, result.end)
        assertEquals(participant.purged, result.purged)
        assertEquals(participant.visible, result.visible)
        assertEquals(participant.creation, result.creation)
        assertEquals(participant.lastEdition, result.lastEdition)
    }

    @Test
    fun `Should toDto with major case true`() {
        // Arrange
        val participant = ParticipantModel().apply {
            birthday = LocalDate.now().minusYears(18L)
        }

        // Act
        val result = mapper.toDto(participant, Locale.getDefault())

        // Assert
        assertEquals(participant.birthday, result.birthday)
        assertEquals(true, result.major)
    }
}
