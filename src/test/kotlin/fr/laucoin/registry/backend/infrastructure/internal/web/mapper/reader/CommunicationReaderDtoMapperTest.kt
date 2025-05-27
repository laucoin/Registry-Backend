package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class CommunicationReaderDtoMapperTest {
    private val projectMapper: ProjectReaderDtoMapper = mock()
    private val movementMapper: MovementReaderDtoMapper = mock()
    private val alertMapper: AlertReaderDtoMapper = mock()
    private val mapper: CommunicationReaderDtoMapper = CommunicationReaderDtoMapper(projectMapper, movementMapper, alertMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert CommunicationModel to CommunicationReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    CommunicationModel().apply {
                        id = UUID.randomUUID()
                        project = ProjectModel()
                        movement = MovementModel()
                        dateTime = ZonedDateTime.now()
                        message = "This is an Communication very interesting"
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    1,
                    1,
                ),
                Arguments.of(
                    CommunicationModel().apply {
                        id = UUID.randomUUID()
                        dateTime = ZonedDateTime.now()
                        message = "This is an Communication very interesting"
                        visible = true
                        creation = HistoryModel()
                        lastEdition = HistoryModel()
                    },
                    0,
                    0,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert CommunicationModel to CommunicationReaderDto`(
        communication: CommunicationModel,
        expectedProjectCast: Int,
        expectedMovementCast: Int,
    ) {
        // Act
        val result = mapper.toDto(communication, Locale.getDefault())

        // Assert
        verify(projectMapper, times(expectedProjectCast)).toDto(communication.project ?: ProjectModel(), Locale.getDefault())
        verify(movementMapper, times(expectedMovementCast)).toDto(communication.movement ?: MovementModel(), Locale.getDefault())

        assertEquals(communication.id, result.id)
        assertEquals(communication.dateTime, result.dateTime)
        assertEquals(communication.message, result.message)
        assertEquals(communication.visible, result.visible)
        assertEquals(communication.creation, result.creation)
        assertEquals(communication.lastEdition, result.lastEdition)
    }
}
