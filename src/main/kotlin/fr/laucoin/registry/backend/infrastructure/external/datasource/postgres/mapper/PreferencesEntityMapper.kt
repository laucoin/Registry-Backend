package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithModel
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

    private fun mapSelectedProfileEntity(entity: PreferencesEntity): ProjectProfileModel? {
        return if (Objects.isNull(entity.selectedProfileId)) null
        else ProjectProfileModel().apply {
            id = entity.selectedProfileId
            role = entity.selectedProfileRole
            status = entity.selectedProfileStatus
            startAccess =
                if (Objects.isNull(entity.selectedProfileStartAccessDate)) null
                else CustomDateTimeModel(entity.selectedProfileStartAccessDate !!, entity.selectedProfileStartAccessTime)
            endAccess =
                if (Objects.isNull(entity.selectedProfileEndAccessDate)) null
                else CustomDateTimeModel(entity.selectedProfileEndAccessDate !!, entity.selectedProfileEndAccessTime)
            project = mapProjectEntity(entity)
        }
    }

    private fun mapProjectEntity(entity: PreferencesEntity): ProjectModel? {
        return if (Objects.isNull(entity.selectedProfileProjectId)) null
        else ProjectModel().apply {
            id = entity.selectedProfileId
            name = entity.selectedProfileProjectName
            begin =
                if (Objects.isNull(entity.selectedProfileProjectStartDate)) null
                else CustomDateTimeModel(entity.selectedProfileProjectStartDate !!, entity.selectedProfileProjectStartTime)
            end =
                if (Objects.isNull(entity.selectedProfileProjectEndDate)) null
                else CustomDateTimeModel(entity.selectedProfileProjectEndDate !!, entity.selectedProfileProjectEndTime)
            options = entity.selectedProfileProjectOptions
        }
    }

    override fun toEntity(model: PreferencesModel): PreferencesEntity {
        return PreferencesEntity().apply {
            userId = model.userId
            selectedProfileId = model.selectedProfile?.id
        }.fillWithModel(model)
    }
}
