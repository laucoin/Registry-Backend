package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectStatusModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectStatusReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper

@Mapper(config = DtoMapperConfig::class)
interface ProjectStatusReaderDtoMapper: IGenericReaderDtoMapper<ProjectStatusModel, ProjectStatusReaderDto> {
	override fun toDto(model: ProjectStatusModel): ProjectStatusReaderDto
}
