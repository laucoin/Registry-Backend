package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
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
				.handle { count, sink -> sink.nextOrError(oldMovement, communicationConflict(count)) }
		} else Mono.just(oldMovement)
	}

	private fun communicationConflict(count: Long): RegistryException? {
		if (count <= 0L) return null
		return RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_COMMUNICATION_OUT_OF_MOVEMENT_DATETIME, arrayListOf(count))
	}

	fun validateUpdatableFields(movement: MovementModel, newMovement: MovementModel): Mono<MovementModel> =
		Mono.just(movement).handle<MovementModel> { it, sink -> sink.nextOrError(it, updatableFieldsError(it, newMovement)) }

	private fun updatableFieldsError(movement: MovementModel, newMovement: MovementModel): RegistryException? = when {
		movement.type !== newMovement.type -> RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_UPDATE_CHANGE_TYPE)

		movement.contentType !== newMovement.contentType ->
			RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_UPDATE_CHANGE_CONTENT_TYPE)

		movement.atLeastOldGuestIfGuestsEntrance(newMovement) ->
			RegistryException(UNPROCESSABLE_CONTENT, MOVEMENT_REMOVE_GUEST_CONTENT)

		else -> null
	}

	fun validateIsAlterable(movement: MovementModel, errorMessage: String): Mono<MovementModel> =
		Mono.just(movement).handle<MovementModel> { it, sink -> sink.nextOrError(it, alterableError(it, errorMessage)) }

	private fun alterableError(movement: MovementModel, errorMessage: String): RegistryException? {
		return if (movement.isLastParticipantMovement()) RegistryException(UNPROCESSABLE_CONTENT, errorMessage) else null
	}
}
