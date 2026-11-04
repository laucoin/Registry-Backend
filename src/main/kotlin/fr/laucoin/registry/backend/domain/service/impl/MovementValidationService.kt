package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_ACTIVITY_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_REMOVE_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_TYPE
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IMovementValidationService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

/**
* Objectif : Applique les règles de validation de la date, de l'activité, des communications et de la modifiabilité
 *   d'un mouvement.
* Périmètre : Date (futur, plage du projet), activité, conflit de communication, champs modifiables et mouvement non
 *   verrouillé.
 * Limites : Ne contrôle pas le contenu (participants, véhicules) ; ne persiste rien.
 */
@Service
class MovementValidationService(
	private val projectService: IProjectService,
	private val activityPort: IActivityPort,
	private val communicationPort: ICommunicationPort,
	clock: Clock,
	@param:Value($$"${registry.feature.movement.future-tolerance}")
	private val futureTolerance: Duration,
) : IMovementValidationService, GenericService(clock) {
	override fun validateMovementDate(movement: MovementModel): Mono<UUID> {
		return validateMovementIsNotInFuture(movement)
			.flatMap { validateMovementDateInProject(it) }
	}

	private fun validateMovementIsNotInFuture(movement: MovementModel): Mono<MovementModel> {
		return if (movement.dateTime.isAfter(ZonedDateTime.now(clock).plus(futureTolerance)))
			Mono.error(RegistryException(BAD_REQUEST, MOVEMENT_DATETIME_IN_FUTURE))
		else Mono.just(movement)
	}

	private fun validateMovementDateInProject(movement: MovementModel): Mono<UUID> {
		return projectService.validateDateTime(
			movement.projectId!!,
			CustomDateTimeModel(movement.dateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
	}

	override fun validateActivity(movement: MovementModel, oldMovement: MovementModel?): Mono<MovementModel> {
		return if (Objects.isNull(movement.activity) || movement.activity?.id == oldMovement?.activity?.id) Mono.just(
			oldMovement ?: movement
		)
		else activityPort.findById(movement.projectId!!, movement.activity!!.id!!, isVisible = null)
			.switchIfEmpty { Mono.error(RegistryException(NOT_FOUND, MOVEMENT_ACTIVITY_NOT_FOUND_IN_MOVEMENT_PROJECT)) }
			.handle { activity, handle ->
				if (activity.isNotVisible()) handle.error(
					RegistryException(
						NOT_FOUND,
						MOVEMENT_ACTIVITY_NOT_VISIBLE,
					)
				)
				else handle.next(oldMovement ?: movement)
			}
	}

	override fun validateNoCommunicationConflict(
		movement: MovementModel,
		oldMovement: MovementModel
	): Mono<MovementModel> {
		return if (movement.dateTime.isAfter(oldMovement.dateTime)) {
			val params = CommunicationSearchParamModel(
				isVisible = null,
				from = null,
				to = movement.dateTime,
			)
			communicationPort.countAllByMovementId(movement.projectId!!, oldMovement.id!!, params)
				.handle { communicationCount, handle ->
					if (communicationCount > 0L) handle.error(
						RegistryException(
							UNPROCESSABLE_CONTENT,
							MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME,
							arrayListOf(communicationCount)
						)
					)
					else handle.next(oldMovement)
				}
		} else Mono.just(oldMovement)
	}

	override fun validateUpdatableFields(oldMovement: MovementModel, newMovement: MovementModel): Mono<MovementModel> =
		Mono.just(oldMovement).handle { movement, handle ->
			when {
				movement.type !== newMovement.type -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_UPDATE_CHANGE_TYPE,
					)
				)

				movement.contentType !== newMovement.contentType -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE,
					)
				)

				movement.atLeastOldGuestIfGuestsEntrance(newMovement) -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_REMOVE_GUEST_CONTENT,
					)
				)

				else -> handle.next(movement)
			}
		}

	override fun validateAlterable(movement: MovementModel, errorMessage: String): Mono<MovementModel> =
		Mono.just(movement).handle { movement, handle ->
			when {
				movement.isLastParticipantMovement() -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						errorMessage,
					)
				)

				else -> handle.next(movement)
			}
		}
}
