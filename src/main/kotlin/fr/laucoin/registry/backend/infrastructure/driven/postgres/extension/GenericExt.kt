package fr.laucoin.registry.backend.infrastructure.driven.postgres.extension

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.GenericProjectModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID

/**
 * Objectif : Renseigne les champs d'audit partagés entre modèles et entités.
 * Périmètre : Mappe les traces de création et de dernière édition et l'id du projet.
 * Limites : Ne mappe que des champs ; ni requête ni règle métier.
 */
object GenericExt {
	fun <M: GenericProjectModel, E: GenericProjectEntity> M.fillWithProjectAndEntity(entity: E): M {
		projectId = entity.projectId

		fillWithEntity(entity)

		return this
	}

	fun <M: GenericModel, E: GenericEntity> M.fillWithEntity(entity: E): M {
		id = entity.id
		visible = entity.visible ?: visible

		creation = historyOf(
			entity.createdAt,
			entity.creatorId,
			entity.creatorFirstName,
			entity.creatorLastName,
			entity.creatorEmail,
		)
		lastEdition = historyOf(
			entity.lastUpdateAt,
			entity.lastEditorId,
			entity.lastEditorFirstName,
			entity.lastEditorLastName,
			entity.lastEditorEmail,
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
		visible = model.visible

		createdAt = model.creation?.dateTime
		creatorId = model.creation?.user?.id

		lastUpdateAt = model.lastEdition?.dateTime
		lastEditorId = model.lastEdition?.user?.id

		return this
	}

	private fun historyOf(
		dateTime: ZonedDateTime?,
		userId: UUID?,
		firstName: String?,
		lastName: String?,
		email: String?,
	): HistoryModel? {
		if (Objects.isNull(dateTime)) return null

		return HistoryModel(
			dateTime = dateTime!!,
			user = if (Objects.nonNull(userId)) HistoryUserModel(userId, firstName, lastName, email) else null,
		)
	}
}
