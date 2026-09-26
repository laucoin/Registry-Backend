package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class PreferencesService(
	private val port: IPreferencesPort,
	@param:Value($$"${registry.information.locale.supported}")
	private val supportedLocales: List<String>,
): IPreferencesService, GenericService() {
	override fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel> {
		return port.findByUserId(currentUser.id!!, visibilitySearched = null)
			.switchIfEmpty {
				val preferences = PreferencesModel(userId = currentUser.id)
				preferences.create(currentUser)
				port.save(preferences)
					.flatMap { port.findByUserId(currentUser.id!!, visibilitySearched = null) }
			}
	}

	override fun updateTheme(
		currentUser: CurrentUserModel,
		theme: ThemeEnum
	): Mono<PreferencesModel> {
		return findByUser(currentUser).flatMap {
			if (it.theme !== theme) {
				it.theme = theme
				port.save(it.apply { update(currentUser) })
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
				port.save(it.apply { update(currentUser) })
			} else Mono.just(it)
		}
	}

}
