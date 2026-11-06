package fr.laucoin.registry.backend.domain.service.core

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Contrat d'écriture des mouvements.
 * Périmètre : Déclare la création, la modification, l'activation, la désactivation, la suppression et la purge des mouvements.
 * Limites : Contrat uniquement ; les lectures sont dans IMovementQueryService et IMovementSearchService.
 */
interface IMovementService {
	fun createMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel> = emptyList()
	): Mono<MovementModel>

	fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: MovementModel,
		newGuests: List<ParticipantModel> = emptyList()
	): Mono<MovementModel>

	fun disableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel>

	fun enableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel>

	fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit>

	fun purgeMovementsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID>
}
