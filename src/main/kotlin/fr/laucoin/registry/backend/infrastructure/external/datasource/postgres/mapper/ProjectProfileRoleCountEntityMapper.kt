package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileRoleCountEntity
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ProjectProfileRoleCountEntityMapper: IEntityMapper<ProjectProfileRoleCountModel, ProjectProfileRoleCountEntity> {
    override fun toModel(entity: ProjectProfileRoleCountEntity): ProjectProfileRoleCountModel {
        return ProjectProfileRoleCountModel().apply {
            project = mapProjectEntity(entity)
            level0 = entity.level0
        }
    }

    fun mapProjectEntity(entity: ProjectProfileRoleCountEntity): ProjectModel? {
        return if (Objects.isNull(entity.projectId)) null
        else ProjectModel().apply {
            id = entity.projectId
            name = entity.projectName
        }
    }

    override fun toEntity(model: ProjectProfileRoleCountModel): ProjectProfileRoleCountEntity {
        return ProjectProfileRoleCountEntity(
            projectId = model.project?.id,
            projectName = model.project?.name,
            level0 = model.level0,
        )
    }
}
