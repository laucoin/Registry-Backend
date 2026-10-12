package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import org.springframework.stereotype.Component

@Component
class ProjectContentPurgeReaderDtoMapper : IGenericReaderDtoMapper<ProjectContentPurgeModel, ProjectContentPurgeReaderDto> {
	override fun toDto(model: ProjectContentPurgeModel): ProjectContentPurgeReaderDto {
		return ProjectContentPurgeReaderDto(model.movements, model.alerts, model.communications)
	}
}
