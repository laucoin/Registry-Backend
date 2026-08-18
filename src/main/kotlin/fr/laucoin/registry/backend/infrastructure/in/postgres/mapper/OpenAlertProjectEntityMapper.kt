package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.OpenAlertProjectEntity
import org.springframework.stereotype.Component

@Component
class OpenAlertProjectEntityMapper : IEntityReaderMapper<OpenAlertProjectModel, OpenAlertProjectEntity> {
	override fun toModel(entity: OpenAlertProjectEntity): OpenAlertProjectModel {
		return OpenAlertProjectModel().apply {
			project = entity.projectId?.let { projectId ->
				ProjectModel(name = entity.projectName).apply { id = projectId }
			}
			openAlertCount = entity.openAlertCount
		}
	}
}
