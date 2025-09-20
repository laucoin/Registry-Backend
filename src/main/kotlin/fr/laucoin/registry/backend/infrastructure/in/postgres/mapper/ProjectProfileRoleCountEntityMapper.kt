package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectEntity
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ProjectProfileRoleCountEntityMapper(
	private val projectMapper: ProjectEntityMapper,
): IEntityMapper<ProjectProfileRoleCountModel, ProjectProfileRoleCountEntity> {
	override fun toModel(entity: ProjectProfileRoleCountEntity): ProjectProfileRoleCountModel {
		return ProjectProfileRoleCountModel().apply {
			project = mapProject(entity)
			level0 = entity.level0
		}
	}

	fun mapProject(entity: ProjectProfileRoleCountEntity): ProjectModel? {
		return Optional.ofNullable(entity.projectId).map {
			projectMapper.toModel(
				ProjectEntity().apply {
					id = it
					name = entity.projectName
				}
			)
		}.orElse(null)
	}

	override fun toEntity(model: ProjectProfileRoleCountModel): ProjectProfileRoleCountEntity {
		return ProjectProfileRoleCountEntity(
			projectId = model.project?.id,
			projectName = model.project?.name,
			level0 = model.level0,
		)
	}
}
