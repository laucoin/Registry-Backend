package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Persistence port for [PreferencesModel]: lookup by User and upsert. Implemented by the jOOQ
 * Postgres adapter; the domain only depends on this contract.
 */
interface IPreferencesPort {
	fun findByUserId(userId: UUID, visibilitySearched: Boolean?): Mono<PreferencesModel>
	fun save(preference: PreferencesModel): Mono<PreferencesModel>
}
