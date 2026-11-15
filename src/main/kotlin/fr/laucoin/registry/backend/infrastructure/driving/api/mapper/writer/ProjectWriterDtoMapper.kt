package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [CustomDateTimeWriterDtoMapper::class])
interface ProjectWriterDtoMapper: IGenericWriterDtoMapper<ProjectModel, ProjectWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "name", source = "name")
	@Mapping(target = "begin", source = "begin")
	@Mapping(target = "end", source = "end")
	@Mapping(target = "options", source = "options")
	override fun toModel(dto: ProjectWriterDto): ProjectModel
}
