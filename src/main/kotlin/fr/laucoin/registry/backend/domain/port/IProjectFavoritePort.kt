package fr.laucoin.registry.backend.domain.port

import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Persiste les projets qu'un utilisateur a marqués comme favoris.
 * Périmètre : Ajoute, retire et recherche les favoris d'un utilisateur donné.
 * Limites : Ne vérifie pas que l'utilisateur peut lire le projet ; le service porte cette règle.
 */
interface IProjectFavoritePort {
	fun add(userId: UUID, projectId: UUID): Mono<Unit>
	fun remove(userId: UUID, projectId: UUID): Mono<Unit>
	fun findFavoriteProjectIds(userId: UUID, projectIds: List<UUID>): Flux<UUID>
}
