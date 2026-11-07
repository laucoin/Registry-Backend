package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.NumericRangeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface NumericRangeWriterDtoMapper: IGenericWriterDtoMapper<NumericRangeModel, NumericRangeWriterDto> {
	override fun toModel(dto: NumericRangeWriterDto): NumericRangeModel
}
