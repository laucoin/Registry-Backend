package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.port.IPreferencesPort
import fr.laucoin.registry.backend.domain.port.IProjectProfilePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import java.util.Objects
import java.util.UUID

@Service
class PreferencesService(
	private val port: IPreferencesPort,
	private val projectProfilePort: IProjectProfilePort,
	@param:Value($$"${registry.information.locale.supported}")
	private val supportedLocales: List<String>,
) : IPreferencesService, GenericService() {
	private companion object {
		private val SEARCH_ACTIVE = ProjectProfileSearchParamModel(
			visibilitySearched = true,
			availabilitySearched = true,
			statusSearched = listOf(ACCEPTED)
		)
	}

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

	override fun updateUserPreferenceSelectedProjectProfileById(
		currentUser: CurrentUserModel,
		profileId: UUID?
	): Mono<PreferencesModel> {
		return if (Objects.isNull(profileId)) return selectedProfile(currentUser)
		else projectProfilePort.findProjectProfileByUserIdAndId(
			currentUser.id!!,
			profileId!!,
			visibilitySearched = true
		)
			.notFoundIfEmpty(profileId)
			.flatMap { selectedProfile(currentUser, it) }
	}

	override fun updateUserPreferenceSelectedProjectProfileByProjectId(
		currentUser: CurrentUserModel,
		projectId: UUID
	): Mono<PreferencesModel> {
		return projectProfilePort.findProjectProfileByProjectAndUserId(projectId, currentUser.id!!, SEARCH_ACTIVE)
			.notFoundIfEmpty(projectId)
			.flatMap { selectedProfile(currentUser, it) }
	}

	private fun selectedProfile(
		currentUser: CurrentUserModel,
		profile: ProjectProfileModel? = null
	): Mono<PreferencesModel> {
		return findByUser(currentUser).flatMap {
			if (it.selectedProfile?.id == profile?.id) Mono.just(it)
			else {
				it.selectedProfile = profile
				port.save(it.apply { update(currentUser) })
			}
		}
	}
}
