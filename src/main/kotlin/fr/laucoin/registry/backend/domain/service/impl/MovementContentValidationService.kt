package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DEFINITIVE_DEPARTURE_BEFORE_LATER_MOVEMENTS
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DRIVERS_NOT_MAJOR
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_VEHICLES_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IMovementContentValidationService
import java.time.Clock
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip

/**
 * Objectif : Contrôle le contenu d'un mouvement.
* Périmètre : Existence, visibilité et majorité des conducteurs des participants, véhicules, et départ définitif sans
 *   mouvement postérieur.
 * Limites : Ne contrôle ni la date ni l'activité du mouvement ; ne persiste rien.
 */
@Service
class MovementContentValidationService(
	private val participantPort: IParticipantPort,
	private val vehiclePort: IVehiclePort,
	clock: Clock,
) : IMovementContentValidationService, GenericService(clock) {
	override fun validateNoLaterMovementForLastDeparture(movement: MovementModel): Mono<MovementModel> {
		if (!movement.isLastParticipantMovement()) return Mono.just(movement)

		return participantPort.findAllByIds(movement.projectId!!, movement.participantIds(), isVisible = true)
			.collectList()
			.handle { found, handle ->
				if (found.any { it.lastMovement?.isAfter(movement.dateTime) == true }) {
					handle.error(RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_DEFINITIVE_DEPARTURE_BEFORE_LATER_MOVEMENTS))
				} else {
					handle.next(movement)
				}
			}
	}

	override fun validateNewContent(movement: MovementModel): Mono<*> {
		return Mono.zip(
			validateParticipantsIfAny(
				movement.projectId!!,
				movement,
				movement.content.mapNotNull { c -> c.participant!!.id },
				movement.content.filter { c -> Objects.nonNull(c.vehicle) }.mapNotNull { c -> c.participant!!.id },
			),
			validateVehiclesIfAny(movement.projectId!!, movement, movement.content.mapNotNull { c -> c.vehicle?.id }),
		)
	}

	override fun validateUpdatedContent(projectId: UUID, oldMovement: MovementModel, movement: MovementModel): Mono<*> =
		Mono.zip(
			validateParticipantsIfAny(
				projectId,
				oldMovement,
				oldMovement.getNewContentParticipantIds(movement),
				oldMovement.getNewContentDriverIds(movement),
			),
			validateVehiclesIfAny(projectId, oldMovement, oldMovement.getNewContentVehicleIds(movement)),
		)

	private fun validateParticipantsIfAny(
		projectId: UUID,
		movement: MovementModel,
		participantIds: List<UUID>,
		driverIds: List<UUID>,
	): Mono<MovementModel> =
		if (participantIds.isEmpty()) Mono.just(movement)
		else validateParticipants(projectId, movement, participantIds, driverIds)

	private fun validateVehiclesIfAny(
		projectId: UUID,
		movement: MovementModel,
		vehicleIds: List<UUID>,
	): Mono<MovementModel> =
		if (vehicleIds.isEmpty()) Mono.just(movement) else validateVehicles(projectId, movement, vehicleIds)

	private fun validateParticipants(
		projectId: UUID,
		oldMovement: MovementModel,
		newParticipantIds: List<UUID>,
		driverIds: List<UUID>
	): Mono<MovementModel> {
		return participantPort.findAllByIds(projectId, newParticipantIds, isVisible = null)
			.collectList()
			.handle { found, handle ->
				val error = participantsError(found, newParticipantIds, driverIds)
				if (error != null) handle.error(error) else handle.next(oldMovement)
			}
	}

	private fun participantsError(
		found: List<ParticipantModel>,
		requestedIds: List<UUID>,
		driverIds: List<UUID>,
	): RegistryException? = when {
		found.size != requestedIds.size ->
			RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_PROJECT)

		found.any(ParticipantModel::isNotUsable) ->
			RegistryException(NOT_FOUND, MOVEMENT_PARTICIPANTS_NOT_VISIBLE)

		found.any { p -> !p.birthday.isMajor(clock) && driverIds.contains(p.id) } ->
			RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_DRIVERS_NOT_MAJOR)

		else -> null
	}

	private fun validateVehicles(
		projectId: UUID,
		movement: MovementModel,
		newVehicleIds: List<UUID>
	): Mono<MovementModel> {
		return vehiclePort.findAllByIds(projectId, newVehicleIds, isVisible = null)
			.collectList()
			.handle { vehicles, handle ->
				when {
					vehicles.size != newVehicleIds.size -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT,
						)
					)

					vehicles.any { m -> m.isNotVisible() } -> handle.error(
						RegistryException(
							NOT_FOUND,
							MOVEMENT_VEHICLES_NOT_VISIBLE,
						)
					)

					else -> handle.next(movement)
				}
			}
	}
}
