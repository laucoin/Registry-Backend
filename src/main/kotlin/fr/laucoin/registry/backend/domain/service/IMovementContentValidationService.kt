package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de validation du contenu d'un mouvement.
 * Périmètre : Déclare les contrôles des participants, véhicules et départs définitifs d'un mouvement.
 * Limites : Contrat uniquement.
 */
interface IMovementContentValidationService {
	fun validateNewContent(movement: MovementModel): Mono<*>

	fun validateUpdatedContent(projectId: UUID, oldMovement: MovementModel, movement: MovementModel): Mono<*>

	fun validateNoLaterMovementForLastDeparture(movement: MovementModel): Mono<MovementModel>
}
