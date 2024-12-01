package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IPreferencesService {
    fun findByUser(currentUser: UserModel): Mono<PreferencesModel>

    fun updateUserPreferenceSelectedEventProfileById(currentUser: UserModel, profileId: UUID): Mono<PreferencesModel>
}
