package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.VehicleTimelineModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.VehicleTimelineReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class, uses = [TimelineReaderDtoMapper::class])
interface VehicleTimelineReaderDtoMapper: IGenericReaderDtoMapper<VehicleTimelineModel, VehicleTimelineReaderDto> {
	override fun toDto(model: VehicleTimelineModel): VehicleTimelineReaderDto
}
