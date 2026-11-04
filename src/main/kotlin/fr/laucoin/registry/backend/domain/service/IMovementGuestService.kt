package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat de la gestion des invités d'un mouvement d'entrée.
 * Périmètre : Déclare la création ou la mise à jour des invités saisis avec un mouvement.
 * Limites : Contrat uniquement.
 */
interface IMovementGuestService {
	fun saveGuestsIfNecessary(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		oldMovement: MovementModel,
		guests: List<ParticipantModel>,
	): Mono<MovementModel>
}
