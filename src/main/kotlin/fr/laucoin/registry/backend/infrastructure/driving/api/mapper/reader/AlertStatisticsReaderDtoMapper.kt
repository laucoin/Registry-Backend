package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.AlertStatisticsModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.AlertStatisticsReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface AlertStatisticsReaderDtoMapper: IGenericReaderDtoMapper<AlertStatisticsModel, AlertStatisticsReaderDto> {
	override fun toDto(model: AlertStatisticsModel): AlertStatisticsReaderDto
}
