package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.writer

import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import java.util.UUID
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.writer.ProjectProfileWriterDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class, uses = [CustomDateTimeWriterDtoMapper::class])
interface ProjectProfileWriterDtoMapper: IGenericProjectWriterDtoMapper<ProjectProfileModel, ProjectProfileWriterDto> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "role", source = "dto.role")
	@Mapping(target = "startAccess", source = "dto.startAccess")
	@Mapping(target = "endAccess", source = "dto.endAccess")
	@Mapping(target = "projectId", source = "projectId")
	override fun toModel(dto: ProjectProfileWriterDto, projectId: UUID): ProjectProfileModel
}
