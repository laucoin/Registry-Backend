package fr.laucoin.registry.backend.infrastructure.driven.postgres.extension

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.GenericProjectModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.util.Objects

/**
 * Shared fill helpers for the common columns every entity/model pair has (id, visibility, audit
 * trail, and — for Project-scoped resources — the owning Project id): fills a domain model from an
 * entity, or an entity from a domain model, so each `*EntityMapper` doesn't repeat this boilerplate.
 */
object GenericExt {
	fun <M: GenericProjectModel, E: GenericProjectEntity> M.fillWithProjectAndEntity(entity: E): M {
		projectId = entity.projectId

		fillWithEntity(entity)

		return this
	}

	fun <M: GenericModel, E: GenericEntity> M.fillWithEntity(entity: E): M {
		id = entity.id
		isVisible = entity.isVisible ?: isVisible

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
		projectId = model.projectId

		fillWithModel(model)

		return this
	}

	fun <M: GenericModel, E: GenericEntity> E.fillWithModel(model: M): E {
		id = model.id
		isVisible = model.isVisible

		createdAt = model.creation?.dateTime
		creatorId = model.creation?.user?.id

		lastUpdateAt = model.lastEdition?.dateTime
		lastEditorId = model.lastEdition?.user?.id

		return this
	}
}
