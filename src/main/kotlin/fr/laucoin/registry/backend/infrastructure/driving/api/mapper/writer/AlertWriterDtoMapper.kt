package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.AlertModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.AlertWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface AlertWriterDtoMapper: IGenericProjectWriterDtoMapper<AlertModel, AlertWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "title", source = "dto.title")
	@Mapping(target = "dateTime", source = "dto.dateTime")
	@Mapping(target = "status", source = "dto.status")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: AlertWriterDto, projectId: UUID): AlertModel
}
