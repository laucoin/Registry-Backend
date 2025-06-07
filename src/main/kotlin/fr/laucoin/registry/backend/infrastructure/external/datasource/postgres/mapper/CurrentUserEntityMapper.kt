package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithEntity
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class CurrentUserEntityMapper(
    private val preferencesEntityMapper: PreferencesEntityMapper
): IEntityReaderMapper<CurrentUserModel, CurrentUserEntity> {
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
        return Optional.ofNullable(entity.preferenceId).map {
            preferencesEntityMapper.toModel(PreferencesEntity().apply {
                id = it
                theme = entity.preferenceTheme ?: ThemeEnum.SYSTEM
                language = entity.preferenceLanguage
                selectedProfileId = entity.preferenceSelectedProfileId
                selectedProfileRole = entity.preferenceSelectedProfileRole
                selectedProfileStatus = entity.preferenceSelectedProfileStatus
                selectedProfileStartAccessDate = entity.preferenceSelectedProfileStartAccessDate
                selectedProfileStartAccessTime = entity.preferenceSelectedProfileStartAccessTime
                selectedProfileEndAccessDate = entity.preferenceSelectedProfileEndAccessDate
                selectedProfileEndAccessTime = entity.preferenceSelectedProfileEndAccessTime
                selectedProfileProjectId = entity.preferenceSelectedProfileProjectId
                selectedProfileProjectName = entity.preferenceSelectedProfileProjectName
                selectedProfileProjectStartDate = entity.preferenceSelectedProfileProjectStartDate
                selectedProfileProjectStartTime = entity.preferenceSelectedProfileProjectStartTime
                selectedProfileProjectEndDate = entity.preferenceSelectedProfileProjectEndDate
                selectedProfileProjectEndTime = entity.preferenceSelectedProfileProjectEndTime
                selectedProfileProjectOptions = entity.preferenceSelectedProfileProjectOptions
            })
        }.orElse(null)
    }
}
