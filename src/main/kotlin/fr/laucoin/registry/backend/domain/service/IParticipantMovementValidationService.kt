package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de validation des mouvements liés à un participant.
 * Périmètre : Déclare le contrôle de cohérence entre les dates de présence d'un participant et ses mouvements.
 * Limites : Contrat uniquement.
 */
interface IParticipantMovementValidationService {
	fun validateNoMovementConflict(participant: ParticipantModel, oldParticipant: ParticipantModel): Mono<ParticipantModel>

	fun validateHasNoMovementLinked(participant: ParticipantModel, error: String): Mono<ParticipantModel>
}
