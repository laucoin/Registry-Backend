package fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.VehicleReaderDto
import java.util.Locale
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

class MovementContentReaderDtoMapperTest {
    private val participantMapper: ParticipantReaderDtoMapper = mock()
    private val vehicleMapper: VehicleReaderDtoMapper = mock()
    private val mapper: MovementContentReaderDtoMapper = MovementContentReaderDtoMapper(participantMapper, vehicleMapper)

    companion object {
        @JvmStatic
        fun `Should toDto convert MovementModel to MovementReaderDto`(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(MovementContentModel(), 0, 0),
                Arguments.of(MovementContentModel().apply {
                    poolName = "pool"
                    participant = ParticipantModel()
                    vehicle = VehicleModel()
                }, 1, 1),
            )
        }
    }

    @ParameterizedTest
    @MethodSource
    fun `Should toDto convert MovementModel to MovementReaderDto`(
        content: MovementContentModel,
        expectedParticipantCast: Int,
        expectedVehicleCast: Int,
    ) {
        // Arrange
        whenever(participantMapper.toDto(any(), any())).thenReturn(ParticipantReaderDto())
        whenever(vehicleMapper.toDto(any(), any())).thenReturn(VehicleReaderDto())

        // Act
        val result = mapper.toDto(content, Locale.getDefault())

        // Assert
        assertEquals(content.poolName, result.poolName)
        verify(participantMapper, times(expectedParticipantCast)).toDto(content.participant ?: ParticipantModel(), Locale.getDefault())
        verify(vehicleMapper, times(expectedVehicleCast)).toDto(content.vehicle ?: VehicleModel(), Locale.getDefault())
    }
}
