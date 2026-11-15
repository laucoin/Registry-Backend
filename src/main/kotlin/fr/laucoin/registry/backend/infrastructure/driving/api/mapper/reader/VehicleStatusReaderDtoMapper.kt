package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleStatusModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface VehicleStatusReaderDtoMapper: IGenericReaderDtoMapper<VehicleStatusModel, VehicleStatusReaderDto> {
	override fun toDto(model: VehicleStatusModel): VehicleStatusReaderDto
}
