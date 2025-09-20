package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

interface IPreferencesPort {
	fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesModel>
	fun save(preference: PreferencesModel): Mono<PreferencesModel>
}
