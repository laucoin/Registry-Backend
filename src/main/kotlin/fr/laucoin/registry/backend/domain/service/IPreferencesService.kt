package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import reactor.core.publisher.Mono

/**
 * Use-case entry point for a User's UI preferences: read the current ones and persist their theme
 * or language choice. Callers go through this contract, never the [IPreferencesPort] directly.
 */
interface IPreferencesService {
	fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel>

	fun updateTheme(currentUser: CurrentUserModel, theme: ThemeEnum): Mono<PreferencesModel>
	fun updateLanguage(currentUser: CurrentUserModel, language: String): Mono<PreferencesModel>
}
