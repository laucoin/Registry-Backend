package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.OpenAlertProjectModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.OpenAlertProjectEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectEntity
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class OpenAlertProjectEntityMapper(
	private val projectMapper: ProjectEntityMapper,
) : IEntityMapper<OpenAlertProjectModel, OpenAlertProjectEntity> {
	override fun toModel(entity: OpenAlertProjectEntity): OpenAlertProjectModel {
		return OpenAlertProjectModel().apply {
			project = Optional.ofNullable(entity.projectId).map {
				projectMapper.toModel(
					ProjectEntity().apply {
						id = it
						name = entity.projectName
					}
				)
			}.orElse(null)
			openAlertCount = entity.openAlertCount
		}
	}

	override fun toEntity(model: OpenAlertProjectModel): OpenAlertProjectEntity {
		return OpenAlertProjectEntity(
			projectId = model.project?.id,
			projectName = model.project?.name,
			openAlertCount = model.openAlertCount,
		)
	}
}
