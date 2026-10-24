package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.model.PreferencesModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Purpose: Driven port giving the domain access to the user preferences.
 * Scope: Reads and saves the preferences of a user.
 * Limits: Contract only; the Postgres adapter owns SQL and mapping.
 */
interface IPreferencesPort {
	fun findByUserId(userId: UUID, isVisible: Boolean?): Mono<PreferencesModel>
	fun save(preference: PreferencesModel): Mono<PreferencesModel>
}
