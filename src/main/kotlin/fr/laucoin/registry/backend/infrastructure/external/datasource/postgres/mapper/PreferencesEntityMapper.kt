package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class PreferencesEntityMapper: IEntityMapper<PreferencesModel, PreferencesEntity> {
    override fun toModel(entity: PreferencesEntity): PreferencesModel {
        return PreferencesModel().apply {
            userId = entity.userId
            selectedProfile = mapSelectedProfileEntity(entity)
        }.fillWithEntity(entity)
    }

    private fun mapSelectedProfileEntity(entity: PreferencesEntity): EventProfileModel? {
        return if (Objects.isNull(entity.selectedProfileId)) null
        else EventProfileModel().apply {
            id = entity.selectedProfileId
            role = entity.selectedProfileRole
            status = entity.selectedProfileStatus
            startAccess =
                if (Objects.isNull(entity.selectedProfileStartAccessDate)) null
                else CustomDateTimeModel(entity.selectedProfileStartAccessDate !!, entity.selectedProfileStartAccessTime)
            endAccess =
                if (Objects.isNull(entity.selectedProfileEndAccessDate)) null
                else CustomDateTimeModel(entity.selectedProfileEndAccessDate !!, entity.selectedProfileEndAccessTime)
            event = mapEventEntity(entity)
        }
    }

    private fun mapEventEntity(entity: PreferencesEntity): EventModel? {
        return if (Objects.isNull(entity.selectedProfileEventId)) null
        else EventModel().apply {
            id = entity.selectedProfileId
            name = entity.selectedProfileEventName
            begin =
                if (Objects.isNull(entity.selectedProfileEventStartDate)) null
                else CustomDateTimeModel(entity.selectedProfileEventStartDate !!, entity.selectedProfileEventStartTime)
            end =
                if (Objects.isNull(entity.selectedProfileEventEndDate)) null
                else CustomDateTimeModel(entity.selectedProfileEventEndDate !!, entity.selectedProfileEventEndTime)
            options = entity.selectedProfileEventOptions
        }
    }

    override fun toEntity(model: PreferencesModel): PreferencesEntity {
        return PreferencesEntity().apply {
            userId = model.userId
            selectedProfileId = model.selectedProfile?.id
        }.fillWithModel(model)
    }
}
