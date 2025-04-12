package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IPreferencesService {
    fun findByUser(currentUser: CurrentUserModel): Mono<PreferencesModel>

    fun updateUserPreferenceSelectedEventProfileById(currentUser: CurrentUserModel, profileId: UUID): Mono<PreferencesModel>
    fun updateUserPreferenceSelectedEventProfileByEventId(currentUser: CurrentUserModel, eventId: UUID): Mono<PreferencesModel>
}
