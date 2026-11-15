package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.AlertEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Compose le mouvement et l'alerte liés à une communication lue en base.
 * Périmètre : Extrait les colonnes préfixées puis délègue aux mappers du mouvement et de l'alerte.
 * Limites : Ne copie aucune propriété lui-même ; la copie est faite par MapStruct.
 */
@Component
class CommunicationMappingHelper(
	private val columns: NestedColumnsMapper,
	private val movementMapper: MovementEntityMapper,
	private val alertMapper: AlertEntityMapper,
) {
	@Named("movementOf")
	fun movementOf(entity: CommunicationEntity): MovementModel? {
		return entity.movementId?.let { movementMapper.toModel(columns.toMovementEntity(entity)) }
	}

	@Named("alertOf")
	fun alertOf(entity: CommunicationEntity): AlertModel? {
		return entity.alertId?.let { alertMapper.toModel(columns.toAlertEntity(entity)) }
	}
}
