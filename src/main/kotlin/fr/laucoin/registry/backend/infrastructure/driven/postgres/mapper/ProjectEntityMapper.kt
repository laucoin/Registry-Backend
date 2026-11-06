package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class)
interface ProjectEntityMapper: IEntityMapper<ProjectModel, ProjectEntity> {
	@IgnoreModelAudit
	@Mapping(target = "begin", expression = "java(mapCustomDateTime(entity.getBeginDate(), entity.getBeginTime()))")
	@Mapping(target = "end", expression = "java(mapCustomDateTime(entity.getEndDate(), entity.getEndTime()))")
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "profile", ignore = true)
	@Mapping(target = "favorite", ignore = true)
	@Mapping(target = "counts", ignore = true)
	override fun toModel(entity: ProjectEntity): ProjectModel

	@IgnoreEntityAudit
	@Mapping(target = "beginDate", source = "begin.date")
	@Mapping(target = "beginTime", source = "begin.time")
	@Mapping(target = "endDate", source = "end.date")
	@Mapping(target = "endTime", source = "end.time")
	@Mapping(target = "participants", ignore = true)
	@Mapping(target = "groups", ignore = true)
	@Mapping(target = "acceptedProfiles", ignore = true)
	@Mapping(target = "vehicles", ignore = true)
	@Mapping(target = "activities", ignore = true)
	@Mapping(target = "activeAlerts", ignore = true)
	override fun toEntity(model: ProjectModel): ProjectEntity
}
