package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.profile.ProjectProfileEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithModel
import org.springframework.stereotype.Component

@Component
class ProjectEntityMapper : IEntityMapper<ProjectModel, ProjectEntity> {
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

	fun toCountsModel(entity: ProjectProfileEntity): ProjectModel.ProjectCountsModel? {
		return entity.participantsCount?.let {
			ProjectModel.ProjectCountsModel(
				participants = it.toLong(),
				vehicles = (entity.vehiclesCount ?: 0).toLong(),
				groups = (entity.groupsCount ?: 0).toLong(),
				activities = (entity.activitiesCount ?: 0).toLong(),
				profiles = (entity.profilesCount ?: 0).toLong(),
				ongoingAlerts = (entity.ongoingAlertsCount ?: 0).toLong(),
			)
		}
	}
}
