package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.model.MovementFeatureModel
import org.springframework.stereotype.Component
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_IN_FUTURE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_REMOVE_GUEST_CONTENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_UPDATE_CHANGE_TYPE
import fr.laucoin.registry.backend.domain.helper.ProjectDateHelper
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import java.time.Clock
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import reactor.core.publisher.Mono

/**
 * Objectif : Porte les règles de validité d'un mouvement : date, modifiabilité et cohérence avec les communications.
 * Périmètre : Refuse un mouvement dans le futur ou hors projet, une modification de champs figés et un conflit avec les communications.
 * Limites : Ne modifie aucune donnée.
 */
@Component
class MovementRulesHelper(
	private val projectDates: ProjectDateHelper,
	private val communicationPort: ICommunicationPort,
	private val clock: Clock,
	private val feature: MovementFeatureModel,
) {
	fun validateMovementDate(movement: MovementModel): Mono<UUID> {
		return validateMovementIsNotInFuture(movement)
			.flatMap { validateMovementDateInProject(it) }
	}

	private fun validateMovementIsNotInFuture(movement: MovementModel): Mono<MovementModel> {
		return if (movement.dateTime.isAfter(ZonedDateTime.now(clock).plus(feature.futureTolerance)))
			Mono.error(RegistryException(BAD_REQUEST, MOVEMENT_DATETIME_IN_FUTURE))
		else Mono.just(movement)
	}

	private fun validateMovementDateInProject(movement: MovementModel): Mono<UUID> {
		return projectDates.validateDateTime(
			movement.projectId!!,
			CustomDateTimeModel(movement.dateTime),
			MOVEMENT_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
	}

	fun validateNoCommunicationConflict(
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
				.handle { it, handle ->
					if (it > 0L) handle.error(
						RegistryException(
							UNPROCESSABLE_CONTENT,
							MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME,
							arrayListOf(it)
						)
					)
					else handle.next(oldMovement)
				}
		} else Mono.just(oldMovement)
	}

	fun validateUpdatableFields(movement: MovementModel, newMovement: MovementModel): Mono<MovementModel> =
		Mono.just(movement).handle<MovementModel> { it, handle ->
			when {
				it.type !== newMovement.type -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_UPDATE_CHANGE_TYPE,
					)
				)

				it.contentType !== newMovement.contentType -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE,
					)
				)

				it.atLeastOldGuestIfGuestsEntrance(newMovement) -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						MOVEMENT_REMOVE_GUEST_CONTENT,
					)
				)

				else -> handle.next(it)
			}
		}

	fun validateIsAlterable(movement: MovementModel, errorMessage: String): Mono<MovementModel> =
		Mono.just(movement).handle<MovementModel> { it, handle ->
			when {
				it.isLastParticipantMovement() -> handle.error(
					RegistryException(
						UNPROCESSABLE_CONTENT,
						errorMessage,
					)
				)

				else -> handle.next(it)
			}
		}
}
