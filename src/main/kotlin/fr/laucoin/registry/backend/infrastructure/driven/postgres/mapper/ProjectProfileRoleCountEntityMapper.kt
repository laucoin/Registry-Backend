package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectProfileRoleCountModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.RoleCountMappingHelper
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class, uses = [RoleCountMappingHelper::class])
interface ProjectProfileRoleCountEntityMapper: IEntityMapper<ProjectProfileRoleCountModel, ProjectProfileRoleCountEntity> {
	@Mapping(target = "project", source = "entity", qualifiedByName = ["projectOf"])
	override fun toModel(entity: ProjectProfileRoleCountEntity): ProjectProfileRoleCountModel

	@Mapping(target = "projectId", source = "project.id")
	@Mapping(target = "projectName", source = "project.name")
	override fun toEntity(model: ProjectProfileRoleCountModel): ProjectProfileRoleCountEntity
}
