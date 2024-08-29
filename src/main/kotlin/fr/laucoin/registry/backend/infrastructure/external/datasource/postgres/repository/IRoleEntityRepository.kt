package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ENTITY_ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.EVENT_ROLE_PERMISSION_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.EVENT_ROLE_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_PERMISSION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_PERMISSIONS
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.USER_ROLE_PERMISSION_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.USER_ROLE_TABLE
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface IRoleEntityRepository: ReactiveCrudRepository<RoleEntity, String> {
    @Query(
        """
        SELECT ur.$ENTITY_ROLE_NAME AS $ROLE_NAME, ur.$ROLE_LEVEL, ARRAY_AGG(t.$ROLE_PERMISSION) AS $ROLE_PERMISSIONS
        FROM $USER_ROLE_PERMISSION_TABLE t
        INNER JOIN $USER_ROLE_TABLE ur ON ur.$ENTITY_ROLE_NAME = t.$ROLE_NAME
        GROUP BY ur.$ENTITY_ROLE_NAME, ur.$ROLE_LEVEL
        ORDER BY ur.$ENTITY_ROLE_NAME
        """
    )
    fun findUserRoles(): Flux<RoleEntity>

    @Query(
        """
        SELECT er.$ENTITY_ROLE_NAME AS $ROLE_NAME, er.$ROLE_LEVEL, ARRAY_AGG(t.$ROLE_PERMISSION) AS $ROLE_PERMISSIONS
        FROM $EVENT_ROLE_PERMISSION_TABLE t
        INNER JOIN $EVENT_ROLE_TABLE er ON er.$ENTITY_ROLE_NAME = t.$ROLE_NAME
        GROUP BY er.$ENTITY_ROLE_NAME, er.$ROLE_LEVEL
        ORDER BY er.$ENTITY_ROLE_NAME
        """
    )
    fun findEventRoles(): Flux<RoleEntity>
}
