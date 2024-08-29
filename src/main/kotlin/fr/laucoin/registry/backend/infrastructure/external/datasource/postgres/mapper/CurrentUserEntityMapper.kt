package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.CurrentUserEntity
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class CurrentUserEntityMapper: IGenericEntityMapper<CurrentUserModel, CurrentUserEntity> {
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
            else EventProfileModel().apply {
                id = entity.preferenceSelectedProfileId
                role = entity.preferenceSelectedProfileRole
                status = entity.preferenceSelectedProfileStatus
                startAccess = entity.preferenceSelectedProfileStartAccess
                endAccess = entity.preferenceSelectedProfileEndAccess
                event = EventModel().apply {
                    id = entity.preferenceSelectedProfileEventId
                    name = entity.preferenceSelectedProfileEventName
                    begin = entity.preferenceSelectedProfileEventStartTime
                    end = entity.preferenceSelectedProfileEventEndTime
                    options = entity.preferenceSelectedProfileEventOptions
                }
            }
        }
    }

    override fun toEntity(model: CurrentUserModel): CurrentUserEntity {
        return CurrentUserEntity().apply {
            oidcId = model.oidcId
            type = model.type
            firstName = model.firstName
            lastName = model.lastName
            email = model.email
            role = model.role
            birthday = model.birthday
            lastLogin = model.lastLogin
            purged = model.purged
            preferenceId = model.preferences?.id
        }.fillWithModel(model)
    }
}
