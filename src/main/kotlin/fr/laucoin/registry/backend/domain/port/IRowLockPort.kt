package fr.laucoin.registry.backend.domain.port

import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Purpose: Takes a pessimistic lock on a single resource row for the duration of the current transaction.
 * Scope: Locks the row of one lockable resource by id; a missing row is not an error.
 * Limits: Does not read the row and never releases the lock itself, which ends with the transaction.
 */
interface IRowLockPort {
	fun lock(resource: LockableResourceEnum, id: UUID): Mono<Unit>
}
