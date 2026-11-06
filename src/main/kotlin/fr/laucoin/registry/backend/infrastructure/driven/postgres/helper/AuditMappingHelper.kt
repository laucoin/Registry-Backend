package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.GenericModel
import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.HistoryModel.HistoryUserModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericEntity
import java.time.ZonedDateTime
import java.util.UUID
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.springframework.stereotype.Component

/**
 * Objectif : Renseigne les champs d'audit partagés entre modèles et entités après chaque mapping MapStruct.
 * Périmètre : Mappe les traces de création et de dernière édition entre les colonnes aplaties et l'historique du modèle.
 * Limites : Ne mappe que ces champs ; ni requête ni règle métier.
 */
@Component
class AuditMappingHelper {
	@AfterMapping
	fun fillAudit(entity: GenericEntity, @MappingTarget model: GenericModel) {
		model.creation = historyOf(
			entity.createdAt,
			entity.creatorId,
			entity.creatorFirstName,
			entity.creatorLastName,
			entity.creatorEmail,
		)
		model.lastEdition = historyOf(
			entity.lastUpdateAt,
			entity.lastEditorId,
			entity.lastEditorFirstName,
			entity.lastEditorLastName,
			entity.lastEditorEmail,
		)
	}

	@AfterMapping
	fun fillAudit(model: GenericModel, @MappingTarget entity: GenericEntity) {
		entity.createdAt = model.creation?.dateTime
		entity.creatorId = model.creation?.user?.id

		entity.lastUpdateAt = model.lastEdition?.dateTime
		entity.lastEditorId = model.lastEdition?.user?.id
	}

	private fun historyOf(
		dateTime: ZonedDateTime?,
		userId: UUID?,
		firstName: String?,
		lastName: String?,
		email: String?,
	): HistoryModel? {
		if (dateTime == null) return null

		return HistoryModel(
			dateTime = dateTime,
			user = userId?.let { HistoryUserModel(it, firstName, lastName, email) },
		)
	}
}
