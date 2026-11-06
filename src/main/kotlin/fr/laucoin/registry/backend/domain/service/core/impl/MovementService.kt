package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DELETED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_DISABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_ENABLED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_CANNOT_BE_UPDATED
import fr.laucoin.registry.backend.domain.helper.MovementContentHelper
import fr.laucoin.registry.backend.domain.helper.MovementParticipantHelper
import fr.laucoin.registry.backend.domain.helper.MovementPersistenceHelper
import fr.laucoin.registry.backend.domain.helper.MovementRulesHelper
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.IMovementService
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip

/**
 * Objectif : Crée, modifie, active, désactive, supprime et purge les mouvements des participants, groupes et véhicules.
 * Périmètre : Orchestre les validations (date, contenu, activité, invités), la transaction et le rafraîchissement des dates effectives.
 * Limites : Ne gère pas les lectures ni la recherche, ni les définitions des participants ou des groupes.
 */
@Service
class MovementService(
	private val port: IMovementPort,
	private val rules: MovementRulesHelper,
	private val content: MovementContentHelper,
	private val participants: MovementParticipantHelper,
	private val persistence: MovementPersistenceHelper,
	private val transactionalOperator: TransactionalOperator,
	clock: Clock,
) : IMovementService, GenericService(clock) {
	override fun createMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return participants.lock((movement.participantIds() + newGuests.mapNotNull { it.id }).distinct())
			.then(Mono.defer { validateNewMovement(currentUser, movement, newGuests) })
			.flatMap { createMovement(currentUser, movement) }
			.flatMap { participants.refreshEffectiveDates(it) }
			.`as`(transactionalOperator::transactional)
	}

	private fun validateNewMovement(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<*> {
		return Mono.zip(
			rules.validateMovementDate(movement),
			content.validateActivity(movement),
			participants.saveGuestsIfNecessary(currentUser, movement, movement, newGuests),
		)
			.flatMap { content.validateNewMovementContent(movement) }
			.flatMap { content.validateNoLaterMovementForLastDeparture(movement) }
	}

	private fun createMovement(currentUser: CurrentUserModel, movement: MovementModel): Mono<MovementModel> {
		return port.create(movement.apply { create(currentUser, now()) })
	}

	override fun updateMovementById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		movement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return rules.validateMovementDate(movement)
			.flatMap { findLockedMovement(projectId, id, isVisible = null) }
			.flatMap { rules.validateUpdatableFields(it, movement) }
			.flatMap { rules.validateIsAlterable(it, MOVEMENT_CANNOT_BE_UPDATED) }
			.flatMap { rules.validateNoCommunicationConflict(movement, it) }
			.flatMap { validateActivityAndGuests(currentUser, movement, it, newGuests) }
			.flatMap { content.validateUpdatedContent(projectId, it, movement).thenReturn(it) }
			.flatMap { applyUpdate(currentUser, it, movement) }
			.`as`(transactionalOperator::transactional)
	}

	private fun validateActivityAndGuests(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		oldMovement: MovementModel,
		newGuests: List<ParticipantModel>,
	): Mono<MovementModel> {
		return Mono.zip(
			content.validateActivity(movement, oldMovement),
			participants.saveGuestsIfNecessary(currentUser, movement, oldMovement, newGuests),
		).thenReturn(oldMovement)
	}

	private fun applyUpdate(
		currentUser: CurrentUserModel,
		oldMovement: MovementModel,
		movement: MovementModel,
	): Mono<MovementModel> {
		val previousParticipantIds = oldMovement.participantIds()
		return participants.lock((previousParticipantIds + movement.participantIds()).distinct())
			.then(persistence.lock(oldMovement.id!!))
			.then(Mono.fromCallable {
				oldMovement.apply {
					dateTime = movement.dateTime
					reason = movement.reason
					activity = movement.activity
					content = movement.content
				}
			})
			.flatMap(persistence::archive)
			.updateMovement(currentUser)
			.flatMap { participants.refreshEffectiveDates(it, previousParticipantIds) }
	}

	private fun Mono<MovementModel>.updateMovement(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser, now()) })
	}

	override fun disableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
		return findLockedMovement(projectId, id, isVisible = true)
			.flatMap { rules.validateIsAlterable(it, MOVEMENT_CANNOT_BE_DISABLED) }
			.updateVisibility(visibility = false)
			.updateMovement(currentUser)
			.flatMap { participants.refreshEffectiveDates(it) }
			.`as`(transactionalOperator::transactional)
	}

	override fun enableMovementById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<MovementModel> {
		return findLockedMovement(projectId, id, isVisible = false)
			.flatMap { rules.validateIsAlterable(it, MOVEMENT_CANNOT_BE_ENABLED) }
			.updateVisibility(visibility = true)
			.updateMovement(currentUser)
			.flatMap { participants.refreshEffectiveDates(it) }
			.`as`(transactionalOperator::transactional)
	}

	override fun deleteMovementById(projectId: UUID, id: UUID): Mono<Unit> {
		return findLockedMovement(projectId, id, isVisible = null)
			.flatMap { rules.validateIsAlterable(it, MOVEMENT_CANNOT_BE_DELETED) }
			.flatMap { deleteAndRefresh(id, it) }
			.`as`(transactionalOperator::transactional)
	}

	private fun findLockedMovement(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<MovementModel> {
		return persistence.findMovement(projectId, id, isVisible)
			.flatMap { participants.lock(it.participantIds()).thenReturn(it) }
			.flatMap { persistence.findMovement(projectId, id, isVisible) }
	}

	private fun deleteAndRefresh(id: UUID, movement: MovementModel): Mono<Unit> {
		return port.deleteById(id)
			.then(participants.refreshEffectiveDates(movement.participantIds()))
	}

	override fun purgeMovementsIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging movements older than {} and uncommented since {}", dateThreshold, dateThreshold)
		return port.findOlderThanAndUncommentedSince(dateThreshold)
			.purgeEach("movement", dryRun, port::deleteById)
	}
}
