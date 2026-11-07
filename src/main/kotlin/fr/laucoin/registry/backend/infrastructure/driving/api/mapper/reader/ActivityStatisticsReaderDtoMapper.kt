package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ActivityStatisticsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface ActivityStatisticsReaderDtoMapper: IGenericReaderDtoMapper<ActivityStatisticsModel, ActivityStatisticsReaderDto> {
	override fun toDto(model: ActivityStatisticsModel): ActivityStatisticsReaderDto
}
