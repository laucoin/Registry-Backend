package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_LEVEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleFields.ROLE_PERMISSIONS
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column

data class RoleEntity(
    @ReadOnlyProperty
    @Column(ROLE_NAME)
    var role: String,
    @ReadOnlyProperty
    @Column(ROLE_LEVEL)
    var level: Int,
    @ReadOnlyProperty
    @Column(ROLE_PERMISSIONS)
    var permissions: List<String>
)
