package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.domain.model.EventProfileSearchParamModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.repository.IEventProfileModelRepository
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class PreferencesService(
    private val repository: IPreferencesModelRepository,
    private val eventProfileRepository: IEventProfileModelRepository
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

    override fun updateUserPreferenceSelectedEventProfileById(
        currentUser: CurrentUserModel,
        profileId: UUID
    ): Mono<PreferencesModel> {
        return eventProfileRepository.findEventProfileByUserIdAndId(currentUser.id !!, profileId, visibilitySearched = true)
            .notFoundIfEmpty(profileId)
            .selectedProfile(currentUser)
    }

    override fun updateUserPreferenceSelectedEventProfileByEventId(
        currentUser: CurrentUserModel,
        eventId: UUID
    ): Mono<PreferencesModel> {
        val search = EventProfileSearchParamModel(
            visibilitySearched = true,
            availabilitySearched = true,
            statusSearched = listOf(ACCEPTED)
        )
        return eventProfileRepository.findEventProfileByEventAndUserId(eventId, currentUser.id !!, search)
            .notFoundIfEmpty(eventId)
            .selectedProfile(currentUser)
    }

    private fun Mono<EventProfileModel>.selectedProfile(currentUser: CurrentUserModel): Mono<PreferencesModel> = flatMap { profile ->
        findByUser(currentUser).flatMap {
            if (it.selectedProfile?.id == profile.id) Mono.just(it)
            else {
                it.selectedProfile = profile
                repository.save(it.apply { update(currentUser) })
            }
        }
    }
}
