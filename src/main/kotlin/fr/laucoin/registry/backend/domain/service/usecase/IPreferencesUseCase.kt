package fr.laucoin.registry.backend.domain.service.usecase

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage des préférences exposé à la couche REST.
 * Périmètre : Reprend les opérations du service Preferences appelées par les contrôleurs.
 * Limites : Contrat uniquement ; les règles métier vivent dans le service.
 */
interface IPreferencesUseCase {
	fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesModel>
	fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesModel>
	fun updateRefreshMode(currentUser: CurrentUserModel, refreshMode: RefreshModeEnum): Mono<PreferencesModel>
}
