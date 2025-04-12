package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IPreferencesModelRepository {
    fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesModel>
    fun save(preference: PreferencesModel): Mono<PreferencesModel>
}
