package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.enumeration.PresenceStatusEnum.IN
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.LabelDto
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ProjectReaderDto
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

class VehicleReaderDtoMapperTest {
	private val presenceStatusMapper: PresenceStatusReaderDtoMapper = mock()
	private val projectMapper: ProjectReaderDtoMapper = mock()
	private val mapper: VehicleReaderDtoMapper = VehicleReaderDtoMapper(projectMapper, presenceStatusMapper)

	companion object {
		@JvmStatic
		fun `Should toDto convert VehicleModel to VehicleReaderDto`(): Stream<Arguments> {
			return Stream.of(
				Arguments.of(
					VehicleModel(),
					0,
					0,
				),
				Arguments.of(
					VehicleModel().apply {
						project = ProjectModel()
						status = IN
					},
					1,
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
		expectedProjectCast: Int,
	) {
		// Arrange
		whenever(presenceStatusMapper.toDto(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(
			LabelDto(
				"translated",
				"translated"
			)
		)
		whenever(projectMapper.toDto(any(), any())).thenReturn(ProjectReaderDto())

		// Act
		val result = mapper.toDto(vehicle, Locale.getDefault())

		// Assert
		verify(presenceStatusMapper, times(expectedTranslation)).toDto(
			any(), any(), anyOrNull(), anyOrNull(), anyOrNull()
		)
		verify(projectMapper, times(expectedProjectCast)).toDto(vehicle.project ?: ProjectModel(), Locale.getDefault())

		assertEquals(vehicle.id, result.id)
		assertEquals(vehicle.licensePlate, result.licensePlate)
		assertEquals(vehicle.brand, result.brand)
		assertEquals(vehicle.model, result.model)
		assertEquals(vehicle.startAvailability, result.startAvailability)
		assertEquals(vehicle.endAvailability, result.endAvailability)
		assertEquals(vehicle.visible, result.visible)
		assertEquals(vehicle.creation, result.creation)
		assertEquals(vehicle.lastEdition, result.lastEdition)
	}
}
