package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.PARAMETER_FORMAT_INVALID
import fr.laucoin.registry.backend.domain.model.RegistryException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus.BAD_REQUEST
import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.support.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.core.IPreferencesService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.time.Clock

/**
 * Objectif : Gère les préférences de l'utilisateur courant.
 * Périmètre : Crée les préférences par défaut à la première lecture et met à jour le thème, la langue et le mode de rafraîchissement.
 * Limites : Ne lit pas les préférences des autres utilisateurs.
 */
@Service
class PreferencesService(
	private val port: IPreferencesPort,
	private val principalCache: IPrincipalCacheService,
	clock: Clock,
	@param:Value($$"${registry.information.locale.supported}")
	private val supportedLocales: List<String>,
): IPreferencesService, GenericService(clock) {
	override fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel> {
		return port.findByUserId(currentUser.id!!, isVisible = null)
			.switchIfEmpty { createDefault(currentUser) }
	}

	private fun createDefault(currentUser: CurrentUserModel): Mono<PreferencesModel> {
		val preferences = PreferencesModel(userId = currentUser.id)
		preferences.create(currentUser, now())
		return port.save(preferences)
			.flatMap { port.findByUserId(currentUser.id!!, isVisible = null) }
			.onErrorResume(DataIntegrityViolationException::class.java) {
				port.findByUserId(currentUser.id!!, isVisible = null)
			}
	}

	override fun updateTheme(
		currentUser: CurrentUserModel,
		theme: ThemeEnum
	): Mono<PreferencesModel> {
		return findByUser(currentUser).flatMap { changeTheme(currentUser, it, theme) }
	}

	override fun updateLanguage(
		currentUser: CurrentUserModel,
		language: String
	): Mono<PreferencesModel> {
		val supportedLanguage = supportedLocales.firstOrNull { it.equals(language, ignoreCase = true) }
			?: return Mono.error(RegistryException(BAD_REQUEST, PARAMETER_FORMAT_INVALID))

		return findByUser(currentUser).flatMap { changeLanguage(currentUser, it, supportedLanguage) }
	}

	override fun updateRefreshMode(
		currentUser: CurrentUserModel,
		refreshMode: RefreshModeEnum
	): Mono<PreferencesModel> {
		return findByUser(currentUser).flatMap { changeRefreshMode(currentUser, it, refreshMode) }
	}

	private fun changeTheme(currentUser: CurrentUserModel, preferences: PreferencesModel, theme: ThemeEnum) =
		saveIfChanged(currentUser, preferences, preferences.theme !== theme) { preferences.theme = theme }

	private fun changeLanguage(currentUser: CurrentUserModel, preferences: PreferencesModel, language: String) =
		saveIfChanged(currentUser, preferences, preferences.language != language) { preferences.language = language }

	private fun changeRefreshMode(currentUser: CurrentUserModel, preferences: PreferencesModel, refreshMode: RefreshModeEnum) =
		saveIfChanged(currentUser, preferences, preferences.refreshMode !== refreshMode) { preferences.refreshMode = refreshMode }

	private fun saveIfChanged(
		currentUser: CurrentUserModel,
		preferences: PreferencesModel,
		changed: Boolean,
		change: () -> Unit,
	): Mono<PreferencesModel> {
		if (!changed) return Mono.just(preferences)
		change()
		return save(currentUser, preferences)
	}

	private fun save(currentUser: CurrentUserModel, preferences: PreferencesModel): Mono<PreferencesModel> {
		return port.save(preferences.apply { update(currentUser, now()) })
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}
}
