package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Compose l'activité liée à un mouvement lu en base.
 * Périmètre : Extrait les colonnes d'activité du mouvement puis délègue au mapper de l'activité.
 * Limites : Ne copie aucune propriété lui-même ; la copie est faite par MapStruct.
 */
@Component
class MovementMappingHelper(
	private val columns: NestedColumnsMapper,
	private val activityMapper: ActivityEntityMapper,
) {
	@Named("activityOf")
	fun activityOf(entity: MovementEntity): ActivityModel? {
		return entity.activityId?.let { activityMapper.toModel(columns.toActivityEntity(entity)) }
	}
}
