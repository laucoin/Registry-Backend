package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.CustomDateTimeWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.SourceParameterCondition

@Mapper(config = DtoMapperConfig::class)
interface CustomDateTimeWriterDtoMapper: IGenericWriterDtoMapper<CustomDateTimeModel?, CustomDateTimeWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "date", source = "date")
	@Mapping(target = "time", source = "time")
	override fun toModel(dto: CustomDateTimeWriterDto): CustomDateTimeModel?

	@SourceParameterCondition
	fun hasDate(dto: CustomDateTimeWriterDto): Boolean = dto.date != null
}
