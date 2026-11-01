package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import org.springframework.stereotype.Component

@Component
class VehicleStatusReaderDtoMapper : IGenericReaderDtoMapper<VehicleStatusModel, VehicleStatusReaderDto> {
	override fun toDto(model: VehicleStatusModel): VehicleStatusReaderDto {
		return VehicleStatusReaderDto(present = model.present, absent = model.absent, lastRefresh = model.lastRefresh)
	}
}
