package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithModel
import org.springframework.stereotype.Component

@Component
class PreferencesEntityMapper: IEntityMapper<PreferencesModel, PreferencesEntity> {
	override fun toModel(entity: PreferencesEntity): PreferencesModel {
		return PreferencesModel().apply {
			userId = entity.userId
			theme = entity.theme
			language = entity.language
			refreshMode = entity.refreshMode
		}.fillWithEntity(entity)
	}

	override fun toEntity(model: PreferencesModel): PreferencesEntity {
		return PreferencesEntity().apply {
			userId = model.userId
			theme = model.theme
			language = model.language
			refreshMode = model.refreshMode
		}.fillWithModel(model)
	}
}
