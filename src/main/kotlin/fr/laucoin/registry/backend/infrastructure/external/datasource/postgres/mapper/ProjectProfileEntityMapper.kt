package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ProjectProfileEntityMapper: IEntityMapper<ProjectProfileModel, ProjectProfileEntity> {
    override fun toModel(entity: ProjectProfileEntity): ProjectProfileModel {
        return ProjectProfileModel().apply {
            user = mapUserEntity(entity)
            role = entity.role
            status = entity.status
            startAccess = if (Objects.isNull(entity.startAccessDate)) null
            else CustomDateTimeModel(entity.startAccessDate !!, entity.startAccessTime)
            endAccess = if (Objects.isNull(entity.endAccessDate)) null
            else CustomDateTimeModel(entity.endAccessDate !!, entity.endAccessTime)
        }.fillWithProjectAndEntity(entity)
    }

    private fun mapUserEntity(entity: ProjectProfileEntity): UserModel? {
        return if (Objects.isNull(entity.userId)) null
        else UserModel().apply {
            id = entity.userId
            firstName = entity.userFirstName
            lastName = entity.userLastName
            email = entity.userEmail
            lastLogin = entity.userLastLogin
            purged = entity.userPurged ?: purged
        }
    }

    override fun toEntity(model: ProjectProfileModel): ProjectProfileEntity {
        return ProjectProfileEntity().apply {
            userId = model.user?.id
            role = model.role
            status = model.status
            startAccessDate = model.startAccess?.date
            startAccessTime = model.startAccess?.time
            endAccessDate = model.endAccess?.date
            endAccessTime = model.endAccess?.time
        }.fillWithProjectAndModel(model)
    }
}
