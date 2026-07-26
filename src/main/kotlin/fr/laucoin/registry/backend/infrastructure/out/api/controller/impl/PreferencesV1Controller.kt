package fr.laucoin.registry.backend.infrastructure.out.api.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.infrastructure.out.api.controller.IPreferencesV1Controller
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.UUID

@RestController
class PreferencesV1Controller(
	private val service: IPreferencesService,
) : IPreferencesV1Controller {
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

	override fun updateSelectedProjectProfile(currentUser: CurrentUserModel, profileId: UUID?): Mono<PreferencesModel> {
		return service.updateUserPreferenceSelectedProjectProfileById(currentUser, profileId)
	}

	override fun updateSelectedProjectProfileWithProjectId(
		currentUser: CurrentUserModel,
		projectId: UUID
	): Mono<PreferencesModel> {
		return service.updateUserPreferenceSelectedProjectProfileByProjectId(currentUser, projectId)
	}
}
