package fr.laucoin.registry.backend.domain.service.impl

import java.time.Clock
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
import fr.laucoin.registry.backend.domain.service.IPrincipalCacheService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

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
			.switchIfEmpty {
				val preferences = PreferencesModel(userId = currentUser.id)
				preferences.create(currentUser, now())
				port.save(preferences)
					.flatMap { port.findByUserId(currentUser.id!!, isVisible = null) }
					.onErrorResume(DataIntegrityViolationException::class.java) {
						port.findByUserId(currentUser.id!!, isVisible = null)
					}
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
		val supportedLanguage = supportedLocales.firstOrNull { it.startsWith(language) }
			?: return Mono.error(RegistryException(BAD_REQUEST, PARAMETER_FORMAT_INVALID))

		return findByUser(currentUser).flatMap {
			if (it.language != supportedLanguage) {
				it.language = supportedLanguage
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
		return port.save(preferences.apply { update(currentUser, now()) })
			.doOnNext { principalCache.invalidate(currentUser.oidcId) }
	}
}
