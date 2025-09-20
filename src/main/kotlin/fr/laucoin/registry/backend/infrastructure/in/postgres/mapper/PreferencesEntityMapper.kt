package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithModel
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class PreferencesEntityMapper(
	private val profileMapper: ProjectProfileEntityMapper
): IEntityMapper<PreferencesModel, PreferencesEntity> {
	override fun toModel(entity: PreferencesEntity): PreferencesModel {
		return PreferencesModel().apply {
			userId = entity.userId
			theme = entity.theme
			language = entity.language
			selectedProfile = mapSelectedProfileEntity(entity)
		}.fillWithEntity(entity)
	}

	private fun mapSelectedProfileEntity(entity: PreferencesEntity): ProjectProfileModel? {
		return Optional.ofNullable(entity.selectedProfileId).map {
			profileMapper.toModel(
				ProjectProfileEntity().apply {
					id = it
					role = entity.selectedProfileRole
					status = entity.selectedProfileStatus
					startAccessDate = entity.selectedProfileStartAccessDate
					startAccessTime = entity.selectedProfileStartAccessTime
					endAccessDate = entity.selectedProfileEndAccessDate
					endAccessTime = entity.selectedProfileEndAccessTime
					projectId = entity.selectedProfileProjectId
					projectName = entity.selectedProfileProjectName
					projectStartDate = entity.selectedProfileProjectStartDate
					projectStartTime = entity.selectedProfileProjectStartTime
					projectEndDate = entity.selectedProfileProjectEndDate
					projectEndTime = entity.selectedProfileProjectEndTime
					projectOptions = entity.selectedProfileProjectOptions
				}
			)
		}.orElse(null)
	}

	override fun toEntity(model: PreferencesModel): PreferencesEntity {
		return PreferencesEntity().apply {
			userId = model.userId
			theme = model.theme
			language = model.language
			selectedProfileId = model.selectedProfile?.id
		}.fillWithModel(model)
	}
}
