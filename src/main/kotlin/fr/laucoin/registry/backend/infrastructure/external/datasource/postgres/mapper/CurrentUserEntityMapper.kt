package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithEntity
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class CurrentUserEntityMapper: IEntityReaderMapper<CurrentUserModel, CurrentUserEntity> {
    override fun toModel(entity: CurrentUserEntity): CurrentUserModel {
        return CurrentUserModel().apply {
            oidcId = entity.oidcId
            type = entity.type ?: type
            firstName = entity.firstName
            lastName = entity.lastName
            email = entity.email
            role = entity.role
            birthday = entity.birthday
            lastLogin = entity.lastLogin
            purged = entity.purged ?: purged
            preferences = mapPreferencesEntity(entity)
        }.fillWithEntity(entity)
    }

    private fun mapPreferencesEntity(entity: CurrentUserEntity): PreferencesModel? {
        return if (Objects.isNull(entity.preferenceId)) null
        else PreferencesModel().apply {
            id = entity.preferenceId
            selectedProfile = if (Objects.isNull(entity.preferenceSelectedProfileId)) null
            else ProjectProfileModel().apply {
                id = entity.preferenceSelectedProfileId
                role = entity.preferenceSelectedProfileRole
                status = entity.preferenceSelectedProfileStatus
                startAccess =
                    if (Objects.isNull(entity.preferenceSelectedProfileStartAccessDate)) null
                    else CustomDateTimeModel(
                        entity.preferenceSelectedProfileStartAccessDate !!,
                        entity.preferenceSelectedProfileStartAccessTime
                    )
                endAccess =
                    if (Objects.isNull(entity.preferenceSelectedProfileEndAccessDate)) null
                    else CustomDateTimeModel(
                        entity.preferenceSelectedProfileEndAccessDate !!,
                        entity.preferenceSelectedProfileEndAccessTime
                    )
                project = ProjectModel().apply {
                    id = entity.preferenceSelectedProfileProjectId
                    name = entity.preferenceSelectedProfileProjectName
                    begin =
                        if (Objects.isNull(entity.preferenceSelectedProfileProjectStartDate)) null
                        else CustomDateTimeModel(
                            entity.preferenceSelectedProfileProjectStartDate !!,
                            entity.preferenceSelectedProfileProjectStartTime
                        )
                    end =
                        if (Objects.isNull(entity.preferenceSelectedProfileProjectEndDate)) null
                        else CustomDateTimeModel(
                            entity.preferenceSelectedProfileProjectEndDate !!,
                            entity.preferenceSelectedProfileProjectEndTime
                        )
                    options = entity.preferenceSelectedProfileProjectOptions
                }
            }
        }
    }
}
