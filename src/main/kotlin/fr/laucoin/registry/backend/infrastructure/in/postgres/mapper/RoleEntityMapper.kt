package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.role.RoleEntity
import org.springframework.stereotype.Component

@Component
class RoleEntityMapper: IEntityMapper<RoleModel, RoleEntity> {
	override fun toModel(entity: RoleEntity): RoleModel {
		return RoleModel(
			entity.role,
			entity.level,
			entity.permissions,
		)
	}

	override fun toEntity(model: RoleModel): RoleEntity {
		return RoleEntity(
			model.role,
			model.level,
			model.permissions,
		)
	}
}
