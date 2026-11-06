package fr.laucoin.registry.backend.domain.helper

import org.springframework.stereotype.Component
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import java.time.Clock
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.NOT_FOUND
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Objectif : Gère les participants d'un mouvement : verrous, dates effectives et invités créés à l'arrivée.
 * Périmètre : Verrouille les participants, rafraîchit leurs dates effectives et enregistre ou met à jour les invités.
 * Limites : Ne valide pas le contenu du mouvement ; c'est le rôle de MovementContentHelper.
 */
@Component
class MovementParticipantHelper(
	private val participantPort: IParticipantPort,
	private val clock: Clock,
) {
	fun lock(ids: List<UUID>): Mono<Unit> {
		return participantPort.lockByIds(ids).then(Mono.just(Unit))
	}

	fun refreshEffectiveDates(movement: MovementModel, previousParticipantIds: List<UUID> = emptyList()): Mono<MovementModel> {
		return participantPort.refreshEffectiveDates((previousParticipantIds + movement.participantIds()).distinct())
			.thenReturn(movement)
	}

	fun refreshEffectiveDates(participantIds: List<UUID>): Mono<Unit> {
		return participantPort.refreshEffectiveDates(participantIds)
	}

	fun saveGuestsIfNecessary(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		oldMovement: MovementModel,
		guests: List<ParticipantModel>,
	): Mono<MovementModel> {
		if (movement.type !== IN || guests.isEmpty()) return Mono.just(oldMovement)

		val guestsToCreate: List<ParticipantModel> = guests
			.filter { Objects.isNull(it.id) }
			.map {
				it.apply {
					startAvailability = CustomDateTimeModel(movement.dateTime)
					create(currentUser, ZonedDateTime.now(clock))
				}
			}

		return findGuestsToUpdate(currentUser, movement, guests)
			.map { it.apply { addAll(guestsToCreate) } }
			.flatMap { saveGuests(it, movement, oldMovement) }
	}

	private fun findGuestsToUpdate(
		currentUser: CurrentUserModel,
		movement: MovementModel,
		guests: List<ParticipantModel>,
	): Mono<MutableList<ParticipantModel>> {
		val guestIdsToUpdate: List<UUID> = guests.mapNotNull { it.id }

		return participantPort.findAllByIds(movement.projectId!!, guestIdsToUpdate, isVisible = null)
			.map { existing -> refreshGuest(existing, guests.find { g -> g.id == existing.id }, movement, currentUser) }
			.collectList()
			.handle { it, handle ->
				if (it.size != guestIdsToUpdate.size) handle.error(
					RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT)
				)
				else handle.next(it)
			}
			.switchIfEmpty { Mono.just(mutableListOf<ParticipantModel>()) }
	}

	private fun refreshGuest(
		existing: ParticipantModel,
		updatedGuest: ParticipantModel?,
		movement: MovementModel,
		currentUser: CurrentUserModel,
	): ParticipantModel {
		return existing.apply {
			firstName = updatedGuest?.firstName
			lastName = updatedGuest?.lastName
			birthday = updatedGuest?.birthday
			startAvailability = CustomDateTimeModel(movement.dateTime)
			update(currentUser, ZonedDateTime.now(clock))
		}
	}

	private fun saveGuests(
		guests: List<ParticipantModel>,
		movement: MovementModel,
		oldMovement: MovementModel,
	): Mono<MovementModel> {
		return participantPort.saveAllGuest(guests)
			.collectList()
			.map { saved ->
				movement.content = saved.map { p -> MovementContentModel(participant = p) }
				oldMovement
			}
	}
}
