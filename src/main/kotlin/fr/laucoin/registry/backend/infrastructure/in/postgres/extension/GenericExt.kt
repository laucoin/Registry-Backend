package fr.laucoin.registry.backend.infrastructure.`in`.postgres.extension

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.GenericProjectModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.generic.GenericProjectEntity
import java.util.Objects

object GenericExt {
	fun <M: GenericProjectModel, E: GenericProjectEntity> M.fillWithProjectAndEntity(entity: E): M {
		project = if (Objects.isNull(entity.projectName)) null
		else ProjectModel().apply {
			id = entity.projectId
			name = entity.projectName
			begin = if (Objects.isNull(entity.projectStartDate)) null
			else CustomDateTimeModel(entity.projectStartDate!!, entity.projectStartTime)
			end = if (Objects.isNull(entity.projectEndDate) && Objects.isNull(entity.projectEndTime)) null
			else CustomDateTimeModel(entity.projectEndDate!!, entity.projectEndTime)
			options = entity.projectOptions
		}

		fillWithEntity(entity)

		return this
	}

	fun <M: GenericModel, E: GenericEntity> M.fillWithEntity(entity: E): M {
		id = entity.id
		visible = entity.visible ?: visible

		creation = if (Objects.isNull(entity.createdAt)) null
		else HistoryModel(
			dateTime = entity.createdAt!!,
			user = if (Objects.nonNull(entity.creatorId)) HistoryUserModel(
				id = entity.creatorId,
				firstName = entity.creatorFirstName,
				lastName = entity.creatorLastName,
				email = entity.creatorEmail
			) else null
		)

		lastEdition = if (Objects.isNull(entity.lastUpdateAt)) null
		else HistoryModel(
			dateTime = entity.lastUpdateAt!!,
			user = if (Objects.nonNull(entity.lastEditorId)) HistoryUserModel(
				id = entity.lastEditorId,
				firstName = entity.lastEditorFirstName,
				lastName = entity.lastEditorLastName,
				email = entity.lastEditorEmail
			) else null
		)

		return this
	}

	fun <M: GenericProjectModel, E: GenericProjectEntity> E.fillWithProjectAndModel(model: M): E {
		projectId = model.project?.id

		fillWithModel(model)

		return this
	}

	fun <M: GenericModel, E: GenericEntity> E.fillWithModel(model: M): E {
		id = model.id
		visible = model.visible

		createdAt = model.creation?.dateTime
		creatorId = model.creation?.user?.id

		lastUpdateAt = model.lastEdition?.dateTime
		lastEditorId = model.lastEdition?.user?.id

		return this
	}
}
