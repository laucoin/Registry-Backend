package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.RoleModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.role.RoleEntity
import org.mapstruct.Mapper

@Mapper(config = PostgresMapperConfig::class)
interface RoleEntityMapper: IEntityMapper<RoleModel, RoleEntity>
