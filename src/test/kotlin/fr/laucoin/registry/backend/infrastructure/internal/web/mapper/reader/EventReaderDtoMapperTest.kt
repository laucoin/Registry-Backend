package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.EVENT_OPTION_NAME_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum.VEHICLE
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
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

class EventReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val mapper: EventReaderDtoMapper = EventReaderDtoMapper(translateService)

    companion object {
        @JvmStatic
        fun `Should toDto convert EventModel to EventReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    EventModel().apply {
                        id = UUID.randomUUID()
                        name = "Name"
                        begin = CustomDateTimeModel(LocalDateTime.MIN)
                        end = CustomDateTimeModel(LocalDateTime.MAX)
                        options = listOf(VEHICLE)
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    1,
                ),
                Arguments.of(
                    EventModel().apply {
                        id = UUID.randomUUID()
                        name = "Name"
                        begin = CustomDateTimeModel(LocalDateTime.MIN)
                        end = CustomDateTimeModel(LocalDateTime.MAX)
                        options = null
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    0,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert EventModel to EventReaderDto`(
        event: EventModel,
        expectedTranslation: Int,
    ) {
        // Arrange
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn("translated")

        // Act
        val result = mapper.toDto(event, Locale.getDefault())

        // Assert
        verify(translateService, times(expectedTranslation)).getMessage(
            "${EVENT_OPTION_NAME_PREFIX}VEHICLE",
            null,
            Locale.getDefault()
        )

        assertEquals(event.id, result.id)
        assertEquals(event.name, result.name)
        assertEquals(event.begin, result.begin)
        assertEquals(event.end, result.end)
        assertEquals(event.options?.size, result.options?.size)
        if ((event.options?.size ?: 0) > 0) {
            for (i in event.options !!.indices) {
                assertEquals(event.options !![i].name, result.options !![i].value)
            }
        }
        assertEquals(event.options?.size, result.options?.size)
        assertEquals(event.visible, result.visible)
        assertEquals(event.creation, result.creation)
        assertEquals(event.lastEdition, result.lastEdition)
    }
}
