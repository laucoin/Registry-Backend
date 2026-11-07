package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = DtoMapperConfig::class)
interface ProjectContentPurgeReaderDtoMapper: IGenericReaderDtoMapper<ProjectContentPurgeModel, ProjectContentPurgeReaderDto>, PurgeReportDtoMapper {
	@Mapping(target = "movements", qualifiedByName = ["reportedIds"])
	@Mapping(target = "alerts", qualifiedByName = ["reportedIds"])
	@Mapping(target = "communications", qualifiedByName = ["reportedIds"])
	override fun toDto(model: ProjectContentPurgeModel): ProjectContentPurgeReaderDto
}
