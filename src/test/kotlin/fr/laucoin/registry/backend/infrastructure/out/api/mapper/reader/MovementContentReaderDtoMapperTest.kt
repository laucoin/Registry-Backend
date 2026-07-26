package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.test.assertEquals

class MovementContentReaderDtoMapperTest {
	private val participantMapper: ParticipantReaderDtoMapper = mock()
	private val vehicleMapper: VehicleReaderDtoMapper = mock()
	private val mapper: MovementContentReaderDtoMapper =
		MovementContentReaderDtoMapper(participantMapper, vehicleMapper)

	private companion object {
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
		whenever(participantMapper.toDto(any())).thenReturn(ParticipantReaderDto())
		whenever(vehicleMapper.toDto(any())).thenReturn(VehicleReaderDto())

		// Act
		val result = mapper.toDto(content)

		// Assert
		assertEquals(content.poolName, result.poolName)
		verify(participantMapper, times(expectedParticipantCast)).toDto(
			content.participant ?: ParticipantModel(),
		)
		verify(vehicleMapper, times(expectedVehicleCast)).toDto(content.vehicle ?: VehicleModel())
	}
}
