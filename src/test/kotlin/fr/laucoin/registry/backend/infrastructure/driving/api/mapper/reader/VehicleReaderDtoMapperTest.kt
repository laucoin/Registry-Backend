package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.LabelDto
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

class VehicleReaderDtoMapperTest {
	private val presenceStatusMapper: PresenceStatusReaderDtoMapper = mock()
	private val mapper: VehicleReaderDtoMapper = VehicleReaderDtoMapper(presenceStatusMapper)

	private companion object {
		@JvmStatic
		fun `Should toDto convert VehicleModel to VehicleReaderDto`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					VehicleModel(),
					0,
				),
				Arguments.of(
					VehicleModel().apply {
						status = IN
					},
					1,
				),
			)
		}
	}

	@ParameterizedTest
	@MethodSource
	fun `Should toDto convert VehicleModel to VehicleReaderDto`(
		vehicle: VehicleModel,
		expectedTranslation: Int,
	) {
		// Arrange
		whenever(presenceStatusMapper.toDto(any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
			LabelDto(
				"translated",
				"translated"
			)
		)

		// Act
		val result = mapper.toDto(vehicle)

		// Assert
		verify(presenceStatusMapper, times(expectedTranslation)).toDto(
			any(), anyOrNull(), anyOrNull(), anyOrNull()
		)

		assertEquals(vehicle.id, result.id)
		assertEquals(vehicle.licensePlate, result.licensePlate)
		assertEquals(vehicle.brand, result.brand)
		assertEquals(vehicle.model, result.model)
		assertEquals(vehicle.startAvailability, result.startAvailability)
		assertEquals(vehicle.endAvailability, result.endAvailability)
		assertEquals(vehicle.isVisible, result.isVisible)
		assertEquals(vehicle.creation, result.creation)
		assertEquals(vehicle.lastEdition, result.lastEdition)
	}
}
