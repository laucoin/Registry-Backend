package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de cas d'usage des préférences.
 * Périmètre : Déclare ce que les contrôleurs et les autres services peuvent demander à propos des préférences.
 * Limites : Contrat uniquement ; les règles métier vivent dans l'implémentation.
 */
interface IPreferencesService {
	fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel>

	fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesModel>
	fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesModel>
	fun updateRefreshMode(currentUser: CurrentUserModel, refreshMode: RefreshModeEnum): Mono<PreferencesModel>
}
