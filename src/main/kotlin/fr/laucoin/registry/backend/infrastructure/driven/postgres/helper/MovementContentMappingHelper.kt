package fr.laucoin.registry.backend.infrastructure.driven.postgres.helper

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper.NestedColumnsMapper
import org.mapstruct.Named
import org.springframework.stereotype.Component

/**
 * Objectif : Compose le participant et le véhicule d'une ligne de contenu de mouvement lue en base.
 * Périmètre : Extrait les colonnes préfixées puis délègue aux mappers du participant et du véhicule.
 * Limites : Ne copie aucune propriété lui-même ; la copie est faite par MapStruct.
 */
@Component
class MovementContentMappingHelper(
	private val columns: NestedColumnsMapper,
	private val participantMapper: ParticipantEntityMapper,
	private val vehicleMapper: VehicleEntityMapper,
) {
	@Named("participantOf")
	fun participantOf(entity: MovementContentEntity): ParticipantModel? {
		return entity.participantId?.let { participantMapper.toModel(columns.toParticipantEntity(entity)) }
	}

	@Named("vehicleOf")
	fun vehicleOf(entity: MovementContentEntity): VehicleModel? {
		return entity.vehicleId?.let { vehicleMapper.toModel(columns.toVehicleEntity(entity)) }
	}
}
