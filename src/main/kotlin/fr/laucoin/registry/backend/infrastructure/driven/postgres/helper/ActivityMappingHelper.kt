package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.NumericRangeModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityEntity
import kotlin.time.Duration
import org.mapstruct.AfterMapping
import org.mapstruct.MappingTarget
import org.springframework.stereotype.Component

/**
 * Objectif : Mappe la durée et le nombre de participants autorisés d'une activité entre l'entité et le modèle.
 * Périmètre : Convertit la durée texte en Duration (valeur Kotlin illisible par MapStruct) et regroupe les bornes de participants.
 * Limites : Ne valide pas la cohérence des bornes.
 */
@Component
class ActivityMappingHelper {
	@AfterMapping
	fun fillDetails(entity: ActivityEntity, @MappingTarget model: ActivityModel) {
		model.duration = entity.duration?.let(Duration::parse)
		model.allowedParticipants = allowedParticipantsOf(entity)
	}

	@AfterMapping
	fun fillDuration(model: ActivityModel, @MappingTarget entity: ActivityEntity) {
		entity.duration = model.duration?.toString()
	}

	private fun allowedParticipantsOf(entity: ActivityEntity): NumericRangeModel? {
		if (entity.minAllowedParticipants == null && entity.maxAllowedParticipants == null) return null

		return NumericRangeModel(entity.minAllowedParticipants, entity.maxAllowedParticipants)
	}
}
