package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.USER_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_OIDC_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.NOT_PURGED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.NOT_SERVICE_ACCOUNT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.PREFERENCES_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.SELECT_PREFERENCES
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.SELECT_USER_SEARCH
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.USER_TEXT_SEARCH_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.VISIBLE_CLAUSE
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface IUserEntityRepository: ReactiveCrudRepository<UserEntity, UUID> {
    @Query(
        """
        SELECT t.*, $SELECT_USER_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $NOT_SERVICE_ACCOUNT AND $USER_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE
        ORDER BY similarity_score DESC, t.$USER_LAST_NAME
        LIMIT :limit OFFSET :offset
        """
    )
    fun findAll(textSearched: String?, visibilitySearched: Boolean?, limit: Int, offset: Int): Flux<UserEntity>

    @Query(
        """
        SELECT COUNT(t.$ID) FROM $USER_TABLE t
        WHERE $NOT_PURGED_CLAUSE AND $NOT_SERVICE_ACCOUNT AND $USER_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE
        """
    )
    fun countAll(textSearched: String?, visibilitySearched: Boolean?): Mono<Long>

    @Query(
        """
        SELECT t.*, $SELECT_USER_SEARCH, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $NOT_SERVICE_ACCOUNT AND $USER_TEXT_SEARCH_CLAUSE AND $VISIBLE_CLAUSE
        ORDER BY similarity_score DESC, t.$USER_LAST_NAME
        LIMIT :limit
        """
    )
    fun findWithLimit(textSearched: String?, visibilitySearched: Boolean?, limit: Int): Flux<UserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND $NOT_SERVICE_ACCOUNT AND t.$ID = :id AND $VISIBLE_CLAUSE
        """
    )
    fun findById(id: UUID, visibilitySearched: Boolean?): Mono<UserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND t.$USER_TYPE = 'SERVICE_ACCOUNT'
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
    fun findByOidcId(oidcId: UUID, visibilitySearched: Boolean?): Mono<CurrentUserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $USER_ROLE_TABLE ur ON t.$USER_ROLE = ur.$ENTITY_ROLE_NAME AND ur.$ROLE_LEVEL = :roleLevel
        WHERE $NOT_PURGED_CLAUSE AND $NOT_SERVICE_ACCOUNT AND $VISIBLE_CLAUSE
        """
    )
    fun findByRoleLevel(roleLevel: Int, visibilitySearched: Boolean?): Flux<UserEntity>

    @Query("SELECT t.$ID FROM $USER_TABLE t WHERE t.last_login::DATE < :dateThreshold AND $NOT_SERVICE_ACCOUNT")
    fun findUserIdsOlderThanLastLogin(dateThreshold: LocalDate): Flux<UUID>
}
