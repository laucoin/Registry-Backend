package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class VehicleReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val statusMapper: PresenceStatusReaderDtoMapper,
) : IGenericReaderDtoMapper<VehicleModel, VehicleReaderDto> {
	override fun toDto(model: VehicleModel): VehicleReaderDto {
		return VehicleReaderDto(
			licensePlate = model.licensePlate,
			brand = model.brand,
			model = model.model,
			status = Optional.ofNullable(model.status).map {
				statusMapper.toDto(
					it,
					model.lastMovement,
					model.startAvailability,
					model.endAvailability,
				)
			}.orElse(null),
			availabilityWarning = model.availabilityWarning,
			startAvailability = model.startAvailability,
			endAvailability = model.endAvailability,
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map(projectMapper::toDto).orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
