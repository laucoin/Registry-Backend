package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericFields.ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.USER_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.CurrentUserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_OIDC_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_ROLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserFields.USER_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.NOT_PURGED_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.PREFERENCES_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserQueries.SELECT_PREFERENCES
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.CREATOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.LAST_EDITOR_JOIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.ONLY_VISIBLE_CLAUSE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_CREATOR
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.GenericQueries.SELECT_LAST_EDITOR
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
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE
        """
    )
    fun findAll(onlyVisible: Boolean): Flux<UserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE AND t.$ID = :id
        """
    )
    fun findById(id: UUID, onlyVisible: Boolean): Mono<UserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        WHERE $NOT_PURGED_CLAUSE AND t.$USER_TYPE = 'SERVICE_ACCOUNT'
        """
    )
    fun findServiceAccount(): Mono<UserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_PREFERENCES, $SELECT_CREATOR, $SELECT_LAST_EDITOR
        FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN $PREFERENCES_JOIN
        WHERE $ONLY_VISIBLE_CLAUSE AND t.$USER_OIDC_ID = :oidcId
        """
    )
    fun findByOidcId(oidcId: UUID, onlyVisible: Boolean): Mono<CurrentUserEntity>

    @Query(
        """
        SELECT t.*, $SELECT_CREATOR, $SELECT_LAST_EDITOR FROM $USER_TABLE t $CREATOR_JOIN $LAST_EDITOR_JOIN
        INNER JOIN $USER_ROLE_TABLE ur ON t.$USER_ROLE = ur.$ENTITY_ROLE_NAME AND ur.$ROLE_LEVEL = :roleLevel
        WHERE $ONLY_VISIBLE_CLAUSE AND $NOT_PURGED_CLAUSE
        """
    )
    fun findByRoleLevel(roleLevel: Int, onlyVisible: Boolean): Flux<UserEntity>
}
