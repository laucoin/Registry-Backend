package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.PreferencesEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Compose les préférences de l'utilisateur courant lu en base.
 * Périmètre : Extrait les colonnes de préférences (avec leurs valeurs par défaut) puis délègue au mapper des préférences.
 * Limites : Ne copie aucune propriété lui-même ; la copie est faite par MapStruct.
 */
@Component
class CurrentUserMappingHelper(
	private val columns: NestedColumnsMapper,
	private val preferencesMapper: PreferencesEntityMapper,
) {
	@Named("preferencesOf")
	fun preferencesOf(entity: CurrentUserEntity): PreferencesModel? {
		return entity.preferenceId?.let { preferencesMapper.toModel(columns.toPreferencesEntity(entity)) }
	}
}
