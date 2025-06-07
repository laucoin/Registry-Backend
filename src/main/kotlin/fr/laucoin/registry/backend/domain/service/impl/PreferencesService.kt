package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ThemeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileSearchParamModel
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.repository.IProjectProfileModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class PreferencesService(
    private val repository: IPreferencesModelRepository,
    private val projectProfileRepository: IProjectProfileModelRepository,
    @Value("\${registry.information.locale.supported}")
    private val supportedLocales: List<String>,
): IPreferencesService, GenericService() {
    override fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel> {
        return repository.findByUserId(currentUser.id !!, visibilitySearched = null)
            .switchIfEmpty {
                val preferences = PreferencesModel(userId = currentUser.id)
                preferences.create(currentUser)
                repository.save(preferences)
                    .flatMap { repository.findByUserId(currentUser.id !!, visibilitySearched = null) }
            }
    }

    override fun updateTheme(
        currentUser: CurrentUserModel,
        theme: ThemeEnum
    ): Mono<PreferencesModel> {
        return findByUser(currentUser).flatMap {
            if (it.theme !== theme) {
                it.theme = theme
                repository.save(it.apply { update(currentUser) })
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
                repository.save(it.apply { update(currentUser) })
            } else Mono.just(it)
        }
    }

    override fun updateUserPreferenceSelectedProjectProfileById(
        currentUser: CurrentUserModel,
        profileId: UUID?
    ): Mono<PreferencesModel> {
        return if (Objects.isNull(profileId)) return selectedProfile(currentUser)
        else projectProfileRepository.findProjectProfileByUserIdAndId(currentUser.id !!, profileId !!, visibilitySearched = true)
            .notFoundIfEmpty(profileId)
            .flatMap { selectedProfile(currentUser, it) }
    }

    override fun updateUserPreferenceSelectedProjectProfileByProjectId(
        currentUser: CurrentUserModel,
        projectId: UUID
    ): Mono<PreferencesModel> {
        val search = ProjectProfileSearchParamModel(
            visibilitySearched = true,
            availabilitySearched = true,
            statusSearched = listOf(ACCEPTED)
        )
        return projectProfileRepository.findProjectProfileByProjectAndUserId(projectId, currentUser.id !!, search)
            .notFoundIfEmpty(projectId)
            .flatMap { selectedProfile(currentUser, it) }
    }

    private fun selectedProfile(currentUser: CurrentUserModel, profile: ProjectProfileModel? = null): Mono<PreferencesModel> {
        return findByUser(currentUser).flatMap {
            if (it.selectedProfile?.id == profile?.id) Mono.just(it)
            else {
                it.selectedProfile = profile
                repository.save(it.apply { update(currentUser) })
            }
        }
    }
}
