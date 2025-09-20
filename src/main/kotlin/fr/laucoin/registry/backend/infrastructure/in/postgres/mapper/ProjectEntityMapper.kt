package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension.GenericExt.fillWithModel
import org.springframework.stereotype.Component

@Component
class ProjectEntityMapper: IEntityMapper<ProjectModel, ProjectEntity> {
	override fun toModel(entity: ProjectEntity): ProjectModel {
		return ProjectModel().apply {
			name = entity.name
			begin = mapCustomDateTime(entity.beginDate, entity.beginTime)
			end = mapCustomDateTime(entity.endDate, entity.endTime)
			status = buildStatus()
			options = entity.options
		}.fillWithEntity(entity)
	}

	override fun toEntity(model: ProjectModel): ProjectEntity {
		return ProjectEntity().apply {
			name = model.name
			beginDate = model.begin?.date
			beginTime = model.begin?.time
			endDate = model.end?.date
			endTime = model.end?.time
			options = model.options
		}.fillWithModel(model)
	}
}
