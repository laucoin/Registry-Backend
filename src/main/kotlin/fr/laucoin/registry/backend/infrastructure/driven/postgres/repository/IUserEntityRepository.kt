package fr.laucoin.registry.backend.infrastructure.driven.postgres.repository

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.USER_PROJECT_MEMBER_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.FULL_COUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.role.RoleFields.USER_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_EMAIL
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_OIDC_ID
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserFields.USER_TYPE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.NOT_SERVICE_ACCOUNT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.PREFERENCES_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.SELECT_PREFERENCES
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.SELECT_USER_SEARCH
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.USER_ROLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.USER_SORT
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.user.UserQueries.USER_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.driven.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Repository d'entités réactif de la table de l'utilisateur.
 * Périmètre : Porte les requêtes SQL paramétrées de l'utilisateur (filtres, tri, pagination, verrous).
 * Limites : Utilisé uniquement par l'adaptateur Postgres ; renvoie des entités, jamais des modèles du domaine.
 */
@Repository
interface IUserEntityRepository: ReactiveCrudRepository<UserEntity, UUID> {
	@Query(
		"""
        SELECT t.*, $SELECT_USER_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR, COUNT(*) OVER() AS $FULL_COUNT FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_SERVICE_ACCOUNT AND $USER_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $USER_ROLE_CLAUSE
        ORDER BY $USER_SORT similarity_score DESC, t.$USER_LAST_NAME, t.$ID
        LIMIT :limit OFFSET :offset
        """
	)
	fun findAll(query: String?, isVisible: Boolean?, role: String?, sortField: String?,
		isDescending: Boolean?,
		limit: Int, offset: Long): Flux<UserEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_USER_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_SERVICE_ACCOUNT AND $USER_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE AND $USER_PROJECT_MEMBER_CLAUSE
        ORDER BY similarity_score DESC, t.$USER_LAST_NAME
        LIMIT :limit
        """
	)
	fun findWithLimit(query: String?, isVisible: Boolean?, memberOfProjectId: UUID?, limit: Int): Flux<UserEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_SERVICE_ACCOUNT AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
	)
	fun findById(id: UUID, isVisible: Boolean?): Mono<UserEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE t.$USER_TYPE = 'SERVICE_ACCOUNT'
        """
	)
	fun findServiceAccount(): Mono<CurrentUserEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_PREFERENCES, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN $PREFERENCES_JOIN
        WHERE $NOT_SERVICE_ACCOUNT AND t.$USER_OIDC_ID = :oidcId AND $VISIBLE_CLAUSE
        """
	)
	fun findByOidcId(oidcId: UUID, isVisible: Boolean?): Mono<CurrentUserEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_PREFERENCES, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN $PREFERENCES_JOIN
        WHERE $NOT_SERVICE_ACCOUNT AND t.$USER_EMAIL = :email AND $VISIBLE_CLAUSE
        """
	)
	fun findByEmail(email: String, isVisible: Boolean?): Flux<CurrentUserEntity>

	@Query(
		"""
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $USER_ROLE_TABLE ur ON t.$USER_ROLE = ur.$ENTITY_ROLE_NAME AND ur.$ROLE_LEVEL = :roleLevel
        WHERE $NOT_SERVICE_ACCOUNT AND $VISIBLE_CLAUSE
        """
	)
	fun findByRoleLevel(roleLevel: Int, isVisible: Boolean?): Flux<UserEntity>

	@Query(
		"""
        SELECT t.$ID FROM $USER_TABLE t
        INNER JOIN $USER_ROLE_TABLE ur ON t.$USER_ROLE = ur.$ENTITY_ROLE_NAME AND ur.$ROLE_LEVEL = 0
        WHERE $NOT_SERVICE_ACCOUNT
        ORDER BY t.$ID
        FOR UPDATE OF t
        """
	)
	fun lockLevel0Users(): Flux<UUID>

	@Query("SELECT t.$ID FROM $USER_TABLE t WHERE t.last_login::DATE < :dateThreshold AND $NOT_SERVICE_ACCOUNT")
	fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID>
}
