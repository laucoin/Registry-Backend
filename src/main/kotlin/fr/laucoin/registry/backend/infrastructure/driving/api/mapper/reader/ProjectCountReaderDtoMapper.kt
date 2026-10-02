package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectCountsDto
import org.springframework.stereotype.Component

@Component
class ProjectCountReaderDtoMapper : IGenericReaderDtoMapper<ProjectModel.ProjectCountsModel, ProjectCountsDto> {
	override fun toDto(model: ProjectModel.ProjectCountsModel): ProjectCountsDto {
		return ProjectCountsDto(
			participants = model.participants,
			vehicles = model.vehicles,
			groups = model.groups,
			activities = model.activities,
			profiles = model.profiles,
			ongoingAlerts = model.ongoingAlerts,
		)
	}
}
