package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import fr.laucoin.registry.backend.domain.model.ProjectConfigurationPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectConfigurationPurgeReaderDto
import org.springframework.stereotype.Component

@Component
class ProjectConfigurationPurgeReaderDtoMapper :
	IGenericReaderDtoMapper<ProjectConfigurationPurgeModel, ProjectConfigurationPurgeReaderDto> {
	override fun toDto(model: ProjectConfigurationPurgeModel): ProjectConfigurationPurgeReaderDto {
		return ProjectConfigurationPurgeReaderDto(
			model.vehicles.take(PURGE_REPORTED_IDS_MAX),
			model.participants.take(PURGE_REPORTED_IDS_MAX),
			model.activities.take(PURGE_REPORTED_IDS_MAX),
			model.groups.take(PURGE_REPORTED_IDS_MAX),
		)
	}
}
