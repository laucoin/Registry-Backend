package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.role.RoleEntity
import org.springframework.stereotype.Component

@Component
class RoleEntityMapper {
    fun toModel(entity: RoleEntity): RoleModel {
        return RoleModel(
            entity.role,
            entity.level,
            entity.permissions,
        )
    }

    fun toEntity(model: RoleModel): RoleEntity {
        return RoleEntity(
            model.role,
            model.level,
            model.permissions,
        )
    }
}
