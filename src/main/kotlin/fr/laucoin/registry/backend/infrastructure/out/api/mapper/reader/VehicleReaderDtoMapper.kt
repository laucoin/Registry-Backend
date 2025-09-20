package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.VehicleReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class VehicleReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val statusMapper: PresenceStatusReaderDtoMapper,
): IGenericReaderDtoMapper<VehicleModel, VehicleReaderDto> {
	override fun toDto(model: VehicleModel, locale: Locale): VehicleReaderDto {
		return VehicleReaderDto(
			licensePlate = model.licensePlate,
			brand = model.brand,
			model = model.model,
			status = Optional.ofNullable(model.status).map {
				statusMapper.toDto(
					it,
					locale,
					model.lastMovement,
					model.startAvailability,
					model.endAvailability,
				)
			}.orElse(null),
			startAvailability = model.startAvailability,
			endAvailability = model.endAvailability,
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
