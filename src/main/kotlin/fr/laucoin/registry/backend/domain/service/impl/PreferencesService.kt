package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IPreferencesModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IPreferencesService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

@Service
class PreferencesService(
    private val repository: IPreferencesModelRepository,
    private val eventProfileService: IUserEventProfileService,
): IPreferencesService, GenericService() {
    override fun findByUser(currentUser: UserModel): Mono<PreferencesModel> {
        return repository.findByUserId(currentUser.id !!, onlyVisible = true)
            .switchIfEmpty {
                val preferences = PreferencesModel(userId = currentUser.id)
                preferences.create(currentUser)
                repository.save(preferences)
                    .flatMap { repository.findByUserId(currentUser.id !!, onlyVisible = true) }
            }
    }

    override fun updateUserPreferenceSelectedEventProfileById(
        currentUser: UserModel,
        profileId: UUID
    ): Mono<PreferencesModel> {
        return eventProfileService.findUserEventProfileById(currentUser, profileId, onlyVisible = true)
            .notFoundIfEmpty(profileId)
            .flatMap { profile ->
                findByUser(currentUser).flatMap {
                    if (it.selectedProfile?.id == profile.id) Mono.just(it)
                    else {
                        it.selectedProfile = profile
                        repository.save(it.apply { update(currentUser) })
                    }
                }
            }
    }
}
