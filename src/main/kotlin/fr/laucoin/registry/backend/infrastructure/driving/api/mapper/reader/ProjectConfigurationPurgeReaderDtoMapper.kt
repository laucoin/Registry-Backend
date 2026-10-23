package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import org.springframework.stereotype.Component

@Component
class ProjectConfigurationPurgeReaderDtoMapper :
	IGenericReaderDtoMapper<ProjectConfigurationPurgeModel, ProjectConfigurationPurgeReaderDto> {
	override fun toDto(model: ProjectConfigurationPurgeModel): ProjectConfigurationPurgeReaderDto {
		return ProjectConfigurationPurgeReaderDto(model.vehicles, model.participants, model.activities, model.groups)
	}
}
