package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.helper.StatusLabelMappingHelper
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [StatusLabelMappingHelper::class])
interface VehicleReaderDtoMapper: IGenericReaderDtoMapper<VehicleModel, VehicleReaderDto> {
	@Mapping(target = "status", source = "vehicle", qualifiedByName = ["vehicleStatus"])
	override fun toDto(vehicle: VehicleModel): VehicleReaderDto
}
