package fr.laucoin.registry.backend.domain.port

import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Purpose: Persists the projects a user marked as favorite.
 * Scope: Adds, removes and looks up favorites for a given user.
 * Limits: Does not check that the user may read the project; the service owns that rule.
 */
interface IProjectFavoritePort {
	fun add(userId: UUID, projectId: UUID): Mono<Unit>
	fun remove(userId: UUID, projectId: UUID): Mono<Unit>
	fun findFavoriteProjectIds(userId: UUID, projectIds: List<UUID>): Flux<UUID>
}
