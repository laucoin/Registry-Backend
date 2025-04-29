package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileRoleEntity
import org.springframework.stereotype.Component

@Component
class ProjectProfileRoleEntityMapper: IEntityReaderMapper<ProjectProfileRoleModel, ProjectProfileRoleEntity> {
    override fun toModel(entity: ProjectProfileRoleEntity): ProjectProfileRoleModel {
        return ProjectProfileRoleModel(
            role = entity.role,
            projectId = entity.projectId,
            projectOptions = entity.projectOptions,
            projectVisible = entity.projectVisible
        )
    }
}
