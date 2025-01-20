package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any

class MovementContentReaderDtoMapperTest {
    private val participantMapper: ParticipantReaderDtoMapper = mock()
    private val mapper: MovementContentReaderDtoMapper = MovementContentReaderDtoMapper(participantMapper)

    @Test
    fun `Should toDto convert MovementModel to MovementReaderDto`() {
        // Arrange
        val content = MovementContentModel().apply {
            participant = ParticipantModel()
        }

        // Act
        mapper.toDto(content)

        // Assert
        verify(participantMapper, times(1)).toDto(content.participant !!)
    }

    @Test
    fun `Should toDto in cas of null Participant`() {
        // Arrange
        val content = MovementContentModel()

        // Act
        val result = mapper.toDto(content)

        // Assert
        verify(participantMapper, times(0)).toDto(any())

        assertNull(result.participant)
    }
}
