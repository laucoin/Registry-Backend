package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.OutingModel
import fr.laucoin.registry.backend.domain.model.TimelineRangeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.OutingReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.TimelineRangeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface TimelineReaderDtoMapper {
	fun toDto(model: TimelineRangeModel): TimelineRangeReaderDto

	fun toDto(model: OutingModel): OutingReaderDto
}
