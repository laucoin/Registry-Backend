package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_PROJECT_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.favorite.ProjectFavoriteFields.PROJECT_FAVORITE_USER_ID
import java.util.UUID
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Lit et écrit les liens favoris entre utilisateur et projet.
 * Périmètre : Insertion, suppression et recherche idempotentes par ids d'utilisateur et de projet.
 * Limites : Ne porte aucune donnée d'audit ou de visibilité ; les lignes sont supprimées en cascade avec l'utilisateur ou le projet.
 */
@Repository
interface IProjectFavoriteEntityRepository: ReactiveCrudRepository<ProjectFavoriteEntity, UUID> {
	@Modifying
	@Query(
		"""
        INSERT INTO $PROJECT_FAVORITE_TABLE ($PROJECT_FAVORITE_USER_ID, $PROJECT_FAVORITE_PROJECT_ID) VALUES (:userId, :projectId)
        ON CONFLICT ($PROJECT_FAVORITE_USER_ID, $PROJECT_FAVORITE_PROJECT_ID) DO NOTHING
        """
	)
	fun add(userId: UUID, projectId: UUID): Mono<Long>

	@Modifying
	@Query("DELETE FROM $PROJECT_FAVORITE_TABLE WHERE $PROJECT_FAVORITE_USER_ID = :userId AND $PROJECT_FAVORITE_PROJECT_ID = :projectId")
	fun remove(userId: UUID, projectId: UUID): Mono<Long>

	@Query("SELECT $PROJECT_FAVORITE_PROJECT_ID FROM $PROJECT_FAVORITE_TABLE WHERE $PROJECT_FAVORITE_USER_ID = :userId AND $PROJECT_FAVORITE_PROJECT_ID IN (:projectIds)")
	fun findProjectIds(userId: UUID, projectIds: List<UUID>): Flux<UUID>
}
