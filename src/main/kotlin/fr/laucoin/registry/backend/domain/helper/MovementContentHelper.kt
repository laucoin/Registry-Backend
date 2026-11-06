package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import reactor.core.publisher.SynchronousSink
import org.springframework.stereotype.Component
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
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
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IParticipantPort
import fr.laucoin.registry.backend.domain.port.IVehiclePort
import java.time.Clock
import java.util.Objects
import java.util.UUID
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip
import reactor.kotlin.core.publisher.switchIfEmpty

/**
 * Objectif : Valide le contenu d'un mouvement : participants, véhicules, activité et départ définitif.
 * Périmètre : Vérifie l'existence, la visibilité et l'aptitude (conducteurs majeurs) des éléments référencés par le mouvement.
 * Limites : Ne modifie aucune donnée.
 */
@Component
class MovementContentHelper(
	private val participantPort: IParticipantPort,
	private val vehiclePort: IVehiclePort,
	private val activityPort: IActivityPort,
	private val clock: Clock,
) {
	fun validateNewMovementContent(movement: MovementModel): Mono<*> {
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

	fun validateUpdatedContent(projectId: UUID, oldMovement: MovementModel, movement: MovementModel): Mono<*> {
		return Mono.zip(
			validateParticipantsIfAny(
				projectId,
				oldMovement,
				oldMovement.getNewContentParticipantIds(movement),
				oldMovement.getNewContentDriverIds(movement),
			),
			validateVehiclesIfAny(projectId, oldMovement, oldMovement.getNewContentVehicleIds(movement)),
		)
	}

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
			.handle { found, sink -> sink.nextOrError(oldMovement, participantsError(found, newParticipantIds, driverIds)) }
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
			.handle { vehicles, sink -> sink.nextIfVehiclesUsable(vehicles, newVehicleIds, movement) }
	}

	private fun SynchronousSink<MovementModel>.nextIfVehiclesUsable(
		vehicles: List<VehicleModel>,
		newVehicleIds: List<UUID>,
		movement: MovementModel,
	) {
		when {
			vehicles.size != newVehicleIds.size ->
				error(RegistryException(NOT_FOUND, MOVEMENT_VEHICLES_NOT_FOUND_IN_MOVEMENT_PROJECT))

			vehicles.any { it.isNotVisible() } -> error(RegistryException(NOT_FOUND, MOVEMENT_VEHICLES_NOT_VISIBLE))
			else -> next(movement)
		}
	}

	fun validateActivity(movement: MovementModel, oldMovement: MovementModel? = null): Mono<MovementModel> {
		return if (Objects.isNull(movement.activity) || movement.activity?.id == oldMovement?.activity?.id) Mono.just(
			oldMovement ?: movement
		)
		else activityPort.findById(movement.projectId!!, movement.activity!!.id!!, isVisible = null)
			.switchIfEmpty { Mono.error(RegistryException(NOT_FOUND, MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT)) }
			.handle { activity, sink -> sink.nextIfActivityVisible(activity, oldMovement ?: movement) }
	}

	private fun SynchronousSink<MovementModel>.nextIfActivityVisible(activity: ActivityModel, movement: MovementModel) {
		if (activity.isNotVisible()) error(RegistryException(NOT_FOUND, MOVEMENT_ACTIVITY_NOT_VISIBLE))
		else next(movement)
	}

	fun validateNoLaterMovementForLastDeparture(movement: MovementModel): Mono<MovementModel> {
		if (!movement.isLastParticipantMovement()) return Mono.just(movement)

		return participantPort.findAllByIds(movement.projectId!!, movement.participantIds(), isVisible = true)
			.collectList()
			.handle { found, sink -> sink.nextIfNoLaterMovement(found, movement) }
	}

	private fun SynchronousSink<MovementModel>.nextIfNoLaterMovement(found: List<ParticipantModel>, movement: MovementModel) {
		if (found.none { it.lastMovement?.isAfter(movement.dateTime) == true }) return next(movement)
		error(RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_DEFINITIVE_DEPARTURE_BEFORE_LATER_MOVEMENTS))
	}
}
