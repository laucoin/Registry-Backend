package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.preference.PreferencesEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithEntity
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
			})
		}.orElse(null)
	}
}
