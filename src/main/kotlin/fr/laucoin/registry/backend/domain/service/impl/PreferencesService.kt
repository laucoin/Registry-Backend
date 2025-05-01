package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
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
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class PreferencesService(
    private val repository: IPreferencesModelRepository,
    private val projectProfileRepository: IProjectProfileModelRepository
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
