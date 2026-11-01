package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.constant.ValidationLimitsConst.PURGE_REPORTED_IDS_MAX
import fr.laucoin.registry.backend.domain.model.ProjectContentPurgeModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ProjectContentPurgeReaderDto
import org.springframework.stereotype.Component

@Component
class ProjectContentPurgeReaderDtoMapper : IGenericReaderDtoMapper<ProjectContentPurgeModel, ProjectContentPurgeReaderDto> {
	override fun toDto(model: ProjectContentPurgeModel): ProjectContentPurgeReaderDto {
		return ProjectContentPurgeReaderDto(
			model.movements.take(PURGE_REPORTED_IDS_MAX),
			model.alerts.take(PURGE_REPORTED_IDS_MAX),
			model.communications.take(PURGE_REPORTED_IDS_MAX),
		)
	}
}
