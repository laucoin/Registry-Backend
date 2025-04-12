package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GroupReaderDtoMapperTest {
    private val eventMapper: EventReaderDtoMapper = mock()
    private val memberMapper: ParticipantReaderDtoMapper = mock()
    private val mapper: GroupReaderDtoMapper = GroupReaderDtoMapper(eventMapper, memberMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert GroupModel to GroupAndContentReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    GroupModel(
                        members = emptyList(),
                    ).apply {
                        id = UUID.randomUUID()
                        name = "Event"
                        startAvailability = CustomDateTimeModel(LocalDateTime.MIN)
                        startAvailability = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    0,
                ),
                Arguments.of(
                    GroupModel(
                        members = emptyList(),
                    ).apply {
                        id = UUID.randomUUID()
                        event = EventModel()
                        members = listOf(ParticipantModel())
                        name = "Event"
                        startAvailability = CustomDateTimeModel(LocalDateTime.MIN)
                        startAvailability = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert GroupModel to GroupAndContentReaderDto`(
        group: GroupModel,
        expectedEventCast: Int,
    ) {
        // Arrange
        whenever(eventMapper.toDto(any(), any())).thenReturn(EventReaderDto())
        whenever(memberMapper.toDtoList(any(), any())).thenReturn(listOf(ParticipantReaderDto()))

        // Act
        val result = mapper.toDto(group, Locale.getDefault())

        // Assert
        verify(eventMapper, times(expectedEventCast)).toDto(group.event ?: EventModel(), Locale.getDefault())
        verify(memberMapper).toDtoList(group.members, Locale.getDefault())

        assertEquals(group.id, result.id)
        assertEquals(group.name, result.name)
        assertEquals(group.startAvailability, result.startAvailability)
        assertEquals(group.endAvailability, result.endAvailability)
        assertEquals(group.visible, result.visible)
        assertEquals(group.creation, result.creation)
        assertEquals(group.lastEdition, result.lastEdition)
    }
}
