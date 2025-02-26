package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.constant.TranslationKeyConst.MOVEMENT_TYPE_PREFIX
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ActivityReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import java.util.Locale
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

class MovementReaderDtoMapperTest {
    private val translateService: MessageSource = mock()
    private val eventMapper: EventReaderDtoMapper = mock()
    private val activityMapper: ActivityReaderDtoMapper = mock()
    private val movementContentMapper: MovementContentReaderDtoMapper = mock()
    private val mapper: MovementReaderDtoMapper =
        MovementReaderDtoMapper(translateService, eventMapper, activityMapper, movementContentMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert MovementModel to MovementReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(MovementModel(), 0, 0, 0),
                Arguments.of(
                    MovementModel().apply {
                        type = MovementTypeEnum.IN
                        activity = ActivityModel()
                        event = EventModel()
                        content = emptyList()
                    },
                    1,
                    1,
                    1,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert MovementModel to MovementReaderDto`(
        movement: MovementModel,
        expectedTranslation: Int,
        expectedActivityCast: Int,
        expectedEventCast: Int,
    ) {
        // Arrange
        whenever(translateService.getMessage(any(), anyOrNull(), any())).thenReturn("translated")
        whenever(activityMapper.toDto(any(), any())).thenReturn(ActivityReaderDto())
        whenever(eventMapper.toDto(any(), any())).thenReturn(EventReaderDto())
        whenever(movementContentMapper.toDtoList(any(), any())).thenReturn(emptyList())

        // Act
        val result = mapper.toDto(movement, Locale.getDefault())

        // Assert
        verify(translateService, times(expectedTranslation)).getMessage(
            "$MOVEMENT_TYPE_PREFIX${movement.type}",
            null,
            Locale.getDefault(),
        )
        verify(movementContentMapper).toDtoList(movement.content, Locale.getDefault())
        verify(eventMapper, times(expectedEventCast)).toDto(movement.event ?: EventModel(), Locale.getDefault())
        verify(activityMapper, times(expectedActivityCast)).toDto(movement.activity ?: ActivityModel(), Locale.getDefault())

        assertEquals(movement.id, result.id)
        assertEquals(movement.dateTime, result.dateTime)
        assertEquals(movement.type?.name, result.type?.value)
        assertEquals(movement.visible, result.visible)
        assertEquals(movement.creation, result.creation)
        assertEquals(movement.lastEdition, result.lastEdition)
    }
}
