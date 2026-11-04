package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.MovementModel
import java.util.UUID
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat des règles de validation d'un mouvement.
 * Périmètre : Déclare les contrôles de date, d'activité, de contenu, de conflit de communication et de modifiabilité.
 * Limites : Contrat uniquement ; les règles vivent dans l'implémentation.
 */
interface IMovementValidationService {
	fun validateMovementDate(movement: MovementModel): Mono<UUID>

	fun validateActivity(movement: MovementModel, oldMovement: MovementModel? = null): Mono<MovementModel>

	fun validateNoCommunicationConflict(movement: MovementModel, oldMovement: MovementModel): Mono<MovementModel>

	fun validateUpdatableFields(oldMovement: MovementModel, newMovement: MovementModel): Mono<MovementModel>

	fun validateAlterable(movement: MovementModel, errorMessage: String): Mono<MovementModel>
}
