package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Purpose: Manages the preferences of the current user.
 * Scope: Creates the default preferences on first read and updates theme, language and refresh mode.
 * Limits: Does not read preferences of other users.
 */
@Service
class PreferencesService(
	private val port: IPreferencesPort,
	private val principalCache: IPrincipalCacheService,
	@param:Value($$"${registry.information.locale.supported}")
	private val supportedLocales: List<String>,
): IPreferencesService, GenericService() {
	override fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel> {
		return port.findByUserId(currentUser.id!!, isVisible = null)
			.switchIfEmpty {
				val preferences = PreferencesModel(userId = currentUser.id)
				preferences.create(currentUser)
				port.save(preferences)
					.flatMap { port.findByUserId(currentUser.id!!, isVisible = null) }
			}
	}

	override fun updateTheme(
		currentUser: CurrentUserModel,
		theme: ThemeEnum
	): Mono<PreferencesModel> {
		return findByUser(currentUser).flatMap {
			if (it.theme !== theme) {
				it.theme = theme
				save(currentUser, it)
			} else Mono.just(it)
		}
	}

	override fun updateLanguage(
		currentUser: CurrentUserModel,
		language: String
	): Mono<PreferencesModel> {
		val language = supportedLocales.firstOrNull { s -> s.startsWith(language) }
		return findByUser(currentUser).flatMap {
			if (it.language !== language) {
				it.language = language
				save(currentUser, it)
			} else Mono.just(it)
		}
	}

	override fun updateRefreshMode(
		currentUser: CurrentUserModel,
		refreshMode: RefreshModeEnum
	): Mono<PreferencesModel> {
		return findByUser(currentUser).flatMap {
			if (it.refreshMode !== refreshMode) {
				it.refreshMode = refreshMode
				save(currentUser, it)
			} else Mono.just(it)
		}
	}

	private fun save(currentUser: CurrentUserModel, preferences: PreferencesModel): Mono<PreferencesModel> {
		return port.save(preferences.apply { update(currentUser) })
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}
}
