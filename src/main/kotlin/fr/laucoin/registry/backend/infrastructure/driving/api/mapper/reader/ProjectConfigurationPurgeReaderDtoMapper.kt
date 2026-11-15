package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface ProjectConfigurationPurgeReaderDtoMapper: IGenericReaderDtoMapper<ProjectConfigurationPurgeModel, ProjectConfigurationPurgeReaderDto>, PurgeReportDtoMapper {
	@Mapping(target = "vehicles", qualifiedByName = ["reportedIds"])
	@Mapping(target = "participants", qualifiedByName = ["reportedIds"])
	@Mapping(target = "activities", qualifiedByName = ["reportedIds"])
	@Mapping(target = "groups", qualifiedByName = ["reportedIds"])
	override fun toDto(model: ProjectConfigurationPurgeModel): ProjectConfigurationPurgeReaderDto
}
