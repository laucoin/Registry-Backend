package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.impl

import fr.laucoin.registry.backend.domain.port.IProjectFavoritePort
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.IProjectFavoriteEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Adaptateur Postgres du port des favoris de projet.
 * Périmètre : Délègue les écritures et recherches de favoris au repository d'entités.
 * Limites : Ne mappe aucune entité, un favori n'étant qu'une paire d'ids d'utilisateur et de projet.
 */
@Service
class ProjectFavoriteModelPostgresRepository(
	private val repository: IProjectFavoriteEntityRepository,
): IProjectFavoritePort {
	override fun add(userId: UUID, projectId: UUID): Mono<Unit> {
		return repository.add(userId, projectId).thenReturn(Unit)
	}

	override fun remove(userId: UUID, projectId: UUID): Mono<Unit> {
		return repository.remove(userId, projectId).thenReturn(Unit)
	}

	override fun findFavoriteProjectIds(userId: UUID, projectIds: List<UUID>): Flux<UUID> {
		if (projectIds.isEmpty()) {
			return Flux.empty()
		}

		return repository.findProjectIds(userId, projectIds)
	}
}
