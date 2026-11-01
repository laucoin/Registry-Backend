package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleTimelineModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleTimelineReaderDto
import org.springframework.stereotype.Component

@Component
class VehicleTimelineReaderDtoMapper(
	private val timelineMapper: TimelineReaderDtoMapper,
): IGenericReaderDtoMapper<VehicleTimelineModel, VehicleTimelineReaderDto> {
	override fun toDto(model: VehicleTimelineModel): VehicleTimelineReaderDto {
		return VehicleTimelineReaderDto(
			planned = model.planned.map(timelineMapper::toDto),
			actual = model.actual.map(timelineMapper::toDto),
			outSeconds = model.outSeconds,
			usageRate = model.usageRate,
		)
	}
}
