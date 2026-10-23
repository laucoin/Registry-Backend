package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import fr.laucoin.registry.backend.domain.model.GenericModel
import java.time.Instant
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Purpose: Guards an update with the optimistic precondition sent by the client.
 * Scope: Locks the resource row, compares its last edition with the expected ones and runs the update in the same transaction.
 * Limits: Without an expected last edition the update runs unguarded; it never merges conflicting changes.
 */
interface IConcurrencyService {
	fun <M : GenericModel, R : Any> updateIfUnchanged(
		resource: LockableResourceEnum,
		id: UUID,
		expectedLastEditions: List<Instant>?,
		current: () -> Mono<M>,
		action: () -> Mono<R>,
	): Mono<R>
}
