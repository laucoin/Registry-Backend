package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import reactor.core.publisher.Mono

/**
 * Purpose: Use-case contract of the preferences.
 * Scope: Declares what the controllers and other services can ask for preferences.
 * Limits: Contract only; the business rules live in the implementation.
 */
interface IPreferencesService {
	fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel>

	fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesModel>
	fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesModel>
	fun updateRefreshMode(currentUser: CurrentUserModel, refreshMode: RefreshModeEnum): Mono<PreferencesModel>
}
