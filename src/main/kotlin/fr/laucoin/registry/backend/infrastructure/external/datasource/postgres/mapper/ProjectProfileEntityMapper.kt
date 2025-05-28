package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ProjectProfileEntityMapper(
    private val userMapper: UserEntityMapper,
): IEntityMapper<ProjectProfileModel, ProjectProfileEntity> {
    override fun toModel(entity: ProjectProfileEntity): ProjectProfileModel {
        return ProjectProfileModel().apply {
            user = mapUserEntity(entity)
            role = entity.role
            startAccess = mapCustomDateTime(entity.startAccessDate, entity.startAccessTime)
            endAccess = mapCustomDateTime(entity.endAccessDate, entity.endAccessTime)
            status = entity.status
            availabilityStatus = buildStatus()
        }.fillWithProjectAndEntity(entity)
    }

    private fun mapUserEntity(entity: ProjectProfileEntity): UserModel? {
        return Optional.ofNullable(entity.userId).map {
            userMapper.toModel(
                UserEntity().apply {
                    id = it
                    firstName = entity.userFirstName
                    lastName = entity.userLastName
                    email = entity.userEmail
                    lastLogin = entity.userLastLogin
                    purged = entity.userPurged ?: purged
                }
            )
        }.orElse(null)
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
