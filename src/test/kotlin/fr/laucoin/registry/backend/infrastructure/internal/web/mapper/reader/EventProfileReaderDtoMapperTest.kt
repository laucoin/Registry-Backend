package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.BLOCKED
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.PartialUserReaderDto
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.MessageSource

class EventProfileReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val eventMapper: EventReaderDtoMapper = mock()
    private val partialUserMapper: PartialUserReaderDtoMapper = mock()
    private val mapper: EventProfileReaderDtoMapper = EventProfileReaderDtoMapper(translateService, eventMapper, partialUserMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert EventProfileModel to EventProfileReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    EventProfileModel().apply {
                        id = UUID.randomUUID()
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = false
                        status = null
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    BLOCKED.name,
                    1,
                    0,
                    0,
                ),
                Arguments.of(
                    EventProfileModel().apply {
                        id = UUID.randomUUID()
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        status = ACCEPTED
                        visible = false
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    BLOCKED.name,
                    1,
                    0,
                    0,
                ),
                Arguments.of(
                    EventProfileModel().apply {
                        id = UUID.randomUUID()
                        event = EventModel()
                        user = UserModel()
                        role = "ROLE"
                        status = ACCEPTED
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    ACCEPTED.name,
                    1,
                    1,
                    1,
                ),
                Arguments.of(
                    EventProfileModel().apply {
                        id = UUID.randomUUID()
                        event = EventModel()
                        user = UserModel()
                        role = "ROLE"
                        startAccess = CustomDateTimeModel(LocalDateTime.MIN)
                        endAccess = CustomDateTimeModel(LocalDateTime.MAX)
                        status = null
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    null,
                    1,
                    1,
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert EventProfileModel to EventProfileReaderDto`(
        profile: EventProfileModel,
        expectedStatusValue: String?,
        expectedTranslation: Int,
        expectedEventCast: Int,
        expectedUserCast: Int,
    ) {
        // Arrange
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn("translated")
        whenever(eventMapper.toDto(any(), any())).thenReturn(EventReaderDto())
        whenever(partialUserMapper.toDto(any(), any())).thenReturn(PartialUserReaderDto())

        // Act
        val result = mapper.toDto(profile, Locale.getDefault())

        // Assert
        verify(eventMapper, times(expectedEventCast)).toDto(profile.event ?: EventModel(), Locale.getDefault())
        verify(partialUserMapper, times(expectedUserCast)).toDto(profile.user ?: UserModel(), Locale.getDefault())

        assertEquals(profile.id, result.id)
        assertEquals(profile.role, result.role?.value)
        assertEquals(expectedStatusValue, result.status?.value)
        assertEquals(profile.startAccess, profile.startAccess)
        assertEquals(profile.endAccess, profile.endAccess)
        assertEquals(profile.visible, result.visible)
        assertEquals(profile.creation, result.creation)
        assertEquals(profile.lastEdition, result.lastEdition)
    }
}
