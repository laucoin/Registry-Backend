package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.ProjectOptionEnum
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.ProjectCountsModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.project.ProjectEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.extension.GenericExt.fillWithModel
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
			counts = toCounts(entity)
		}.fillWithEntity(entity)
	}

	private fun toCounts(entity: ProjectEntity): ProjectCountsModel? {
		val participants = entity.participants ?: return null
		return ProjectCountsModel(
			participants = participants,
			groups = entity.groups ?: 0,
			acceptedProfiles = entity.acceptedProfiles ?: 0,
			vehicles = entity.countIfEnabled(ProjectOptionEnum.VEHICLE, entity.vehicles),
			activities = entity.countIfEnabled(ProjectOptionEnum.ACTIVITY, entity.activities),
			activeAlerts = entity.countIfEnabled(ProjectOptionEnum.ALERT, entity.activeAlerts),
		)
	}

	private fun ProjectEntity.countIfEnabled(option: ProjectOptionEnum, count: Long?): Long? {
		return count.takeIf { option in options.orEmpty() }
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
