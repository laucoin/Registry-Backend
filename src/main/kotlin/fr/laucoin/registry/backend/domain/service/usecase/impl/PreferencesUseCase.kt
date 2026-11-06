package fr.laucoin.registry.backend.domain.service.usecase.impl

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.core.IPreferencesService
import fr.laucoin.registry.backend.domain.service.usecase.IPreferencesUseCase
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Point d'entrée des préférences pour la couche REST.
 * Périmètre : Délègue chaque opération au service Preferences sans logique propre.
 * Limites : Ne porte aucune règle métier ; elles restent dans le service.
 */
@Service
class PreferencesUseCase(private val service: IPreferencesService) : IPreferencesUseCase {
	override fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesModel> {
		return service.updateTheme(currentUser, theme)
	}

	override fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesModel> {
		return service.updateLanguage(currentUser, language)
	}

	override fun updateRefreshMode(
		currentUser: CurrentUserModel,
		refreshMode: RefreshModeEnum,
	): Mono<PreferencesModel> {
		return service.updateRefreshMode(currentUser, refreshMode)
	}
}
