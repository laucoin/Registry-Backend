package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.time.Duration
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class ActivityReaderDtoMapperTest {
    private val eventMapper: EventReaderDtoMapper = mock()
    private val mapper: ActivityReaderDtoMapper = ActivityReaderDtoMapper(eventMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert ActivityModel to ActivityReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    ActivityModel().apply {
                        id = UUID.randomUUID()
                        event = EventModel()
                        name = "Activity 1"
                        description = "This is an activity very interesting"
                        duration = Duration.ZERO
                        allowedParticipants = NumericRangeModel(lower = 1, upper = 10)
                        startAvailability = CustomDateTimeModel(LocalDateTime.MIN)
                        endAvailability = CustomDateTimeModel(LocalDateTime.MAX)
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    1,
                ),
                Arguments.of(
                    ActivityModel().apply {
                        id = UUID.randomUUID()
                        name = "Activity 1"
                        description = "This is an activity very interesting"
                        allowedParticipants = NumericRangeModel(lower = 1, upper = 10)
                        startAvailability = CustomDateTimeModel(LocalDateTime.MIN)
                        endAvailability = CustomDateTimeModel(LocalDateTime.MAX)
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
    fun `Should toDto convert ActivityModel to ActivityReaderDto`(
        activity: ActivityModel,
        expectedEventCast: Int,
    ) {
        // Act
        val result = mapper.toDto(activity, Locale.getDefault())

        // Assert
        verify(eventMapper, times(expectedEventCast)).toDto(activity.event ?: EventModel(), Locale.getDefault())

        assertEquals(activity.id, result.id)
        assertEquals(activity.name, result.name)
        assertEquals(activity.description, result.description)
        assertEquals(activity.duration?.toIsoString(), result.duration?.value)
        assertEquals(activity.allowedParticipants, result.allowedParticipants)
        assertEquals(activity.startAvailability, result.startAvailability)
        assertEquals(activity.endAvailability, result.endAvailability)
        assertEquals(activity.visible, result.visible)
        assertEquals(activity.creation, result.creation)
        assertEquals(activity.lastEdition, result.lastEdition)
    }
}
