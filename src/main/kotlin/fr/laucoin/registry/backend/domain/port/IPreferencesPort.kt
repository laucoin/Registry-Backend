package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import reactor.core.publisher.Mono
import java.util.UUID

interface IPreferencesPort {
	fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesModel>
	fun save(preference: PreferencesModel): Mono<PreferencesModel>
}
