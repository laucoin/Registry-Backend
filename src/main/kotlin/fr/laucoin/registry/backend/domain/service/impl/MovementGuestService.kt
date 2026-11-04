package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IMovementGuestService
import java.time.Clock
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Objectif : Crée ou met à jour les invités saisis avec un mouvement d'entrée.
* Périmètre : Rattachement des invités au mouvement, date de début de disponibilité et vérification de leur existence.
 * Limites : Ne valide pas le mouvement lui-même ; ne gère que les invités du type d'entrée.
 */
@Service
class MovementGuestService(
	private val participantPort: IParticipantPort,
	clock: Clock,
) : IMovementGuestService, GenericService(clock) {
	override fun saveGuestsIfNecessary(
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
					create(currentUser, now())
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
			.handle { guests, handle ->
				if (guests.size != guestIdsToUpdate.size) handle.error(
					RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT)
				)
				else handle.next(guests)
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
			update(currentUser, now())
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
