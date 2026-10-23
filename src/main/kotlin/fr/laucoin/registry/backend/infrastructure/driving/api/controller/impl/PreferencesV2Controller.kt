package fr.laucoin.registry.backend.infrastructure.driving.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.RefreshModeEnum
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.driving.api.controller.IPreferencesV2Controller
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class PreferencesV2Controller(
	private val service: IPreferencesService,
): IPreferencesV2Controller {
	override fun updateTheme(
		currentUser: CurrentUserModel,
		theme: ThemeEnum
	): Mono<PreferencesModel> {
		return service.updateTheme(currentUser, theme)
	}

	override fun updateLanguage(
		currentUser: CurrentUserModel,
		language: String
	): Mono<PreferencesModel> {
		return service.updateLanguage(currentUser, language)
	}

	override fun updateRefreshMode(
		currentUser: CurrentUserModel,
		mode: RefreshModeEnum
	): Mono<PreferencesModel> {
		return service.updateRefreshMode(currentUser, mode)
	}
}
