package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleEntity
import org.mapstruct.Mapper

@Mapper(config = PostgresMapperConfig::class)
interface ProjectProfileRoleEntityMapper: IEntityReaderMapper<ProjectProfileRoleModel, ProjectProfileRoleEntity>
