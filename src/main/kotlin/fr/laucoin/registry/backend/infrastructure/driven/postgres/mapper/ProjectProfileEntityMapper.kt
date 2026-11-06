package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.ProjectProfileMappingHelper
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class, uses = [ProjectProfileMappingHelper::class])
interface ProjectProfileEntityMapper: IEntityMapper<ProjectProfileModel, ProjectProfileEntity> {
	@IgnoreModelAudit
	@Mapping(target = "user", source = "entity", qualifiedByName = ["userOf"])
	@Mapping(target = "project", expression = "java(entity.getProjectName() == null ? null : toProject(entity))")
	@Mapping(target = "startAccess", expression = "java(mapCustomDateTime(entity.getStartAccessDate(), entity.getStartAccessTime()))")
	@Mapping(target = "endAccess", expression = "java(mapCustomDateTime(entity.getEndAccessDate(), entity.getEndAccessTime()))")
	@Mapping(target = "availabilityStatus", ignore = true)
	override fun toModel(entity: ProjectProfileEntity): ProjectProfileModel

	@BeanMapping(ignoreByDefault = true)
	@KeepGenericProjectColumns
	@Mapping(target = "userId", source = "user.id")
	@Mapping(target = "role", source = "role")
	@Mapping(target = "status", source = "status")
	@Mapping(target = "startAccessDate", source = "startAccess.date")
	@Mapping(target = "startAccessTime", source = "startAccess.time")
	@Mapping(target = "endAccessDate", source = "endAccess.date")
	@Mapping(target = "endAccessTime", source = "endAccess.time")
	override fun toEntity(model: ProjectProfileModel): ProjectProfileEntity

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "projectId")
	@Mapping(target = "name", source = "projectName")
	@Mapping(target = "begin", expression = "java(mapCustomDateTime(entity.getProjectStartDate(), entity.getProjectStartTime()))")
	@Mapping(target = "end", expression = "java(mapCustomDateTime(entity.getProjectEndDate(), entity.getProjectEndTime()))")
	@Mapping(target = "options", source = "projectOptions")
	fun toProject(entity: ProjectProfileEntity): ProjectModel
}
