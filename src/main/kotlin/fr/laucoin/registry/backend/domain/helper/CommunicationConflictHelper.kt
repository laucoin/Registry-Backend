package fr.laucoin.registry.backend.domain.helper

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import reactor.kotlin.core.publisher.switchIfEmpty
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_IS_AFTER_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_IS_NOT_COMPATIBLE_WITH_COMMUNICATION_CREATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_NOT_FOUND_IN_COMMUNICATION_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_ALERT_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_CONTENT_TYPE_NOT_REGISTERED
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_IS_AFTER_COMMUNICATION
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_FOUND_IN_COMMUNICATION_PROJECT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_MOVEMENT_TYPE_NOT_OUT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.NOT_ENOUGH_PERMISSION
import fr.laucoin.registry.backend.domain.constant.ProjectPermissionConst.REGISTRY_PROJECT_OPTION_ALERT
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IAlertPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import java.util.Objects
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Objectif : Vérifie la compatibilité d'une communication avec le mouvement ou l'alerte qu'elle référence.
 * Périmètre : Charge le mouvement ou l'alerte, contrôle sa visibilité, ses dates et son état, et l'option d'alerte du projet.
 * Limites : Ne persiste rien ; ne valide ni la date de la communication ni son contenu propre.
 */
@Component
class CommunicationConflictHelper(
	private val movementPort: IMovementPort,
	private val alertPort: IAlertPort,
) {
	fun validateNoMovementConflict(
		communication: CommunicationModel,
		oldCommunication: CommunicationModel? = null
	): Mono<CommunicationModel> {
		val communicationToReturn = oldCommunication ?: communication
		return if (Objects.isNull(communication.movement) || communication.movement!!.id == oldCommunication?.movement?.id)
			Mono.just(communicationToReturn)
		else movementPort.findById(communication.projectId!!, communication.movement!!.id!!, isVisible = null)
			.switchIfEmpty {
				Mono.error(RegistryException(NOT_FOUND, COMMUNICATION_MOVEMENT_NOT_FOUND_IN_COMMUNICATION_PROJECT))
			}
			.handle { movement, sink -> sink.nextOrError(communicationToReturn, movementConflict(movement, communication)) }
	}

	private fun movementConflict(movement: MovementModel, communication: CommunicationModel): RegistryException? = when {
		movement.isNotVisible() -> RegistryException(NOT_FOUND, COMMUNICATION_MOVEMENT_NOT_VISIBLE)

		movement.dateTime.isAfter(communication.dateTime) ->
			RegistryException(UNPROCESSABLE_CONTENT, COMMUNICATION_MOVEMENT_IS_AFTER_COMMUNICATION)

		movement.contentType !== REGISTERED ->
			RegistryException(UNPROCESSABLE_CONTENT, COMMUNICATION_MOVEMENT_CONTENT_TYPE_NOT_REGISTERED)

		movement.type !== OUT -> RegistryException(UNPROCESSABLE_CONTENT, COMMUNICATION_MOVEMENT_TYPE_NOT_OUT)

		else -> null
	}

	fun validateNoAlertConflict(
		currentUser: CurrentUserModel,
		communication: CommunicationModel,
		oldCommunication: CommunicationModel? = null
	): Mono<CommunicationModel> {
		val communicationToReturn = oldCommunication ?: communication
		val alertId = communication.alert?.id
		if (alertId == null || alertId == oldCommunication?.alert?.id) return Mono.just(communicationToReturn)
		if (!currentUser.hasAuthority(communication.projectId!!, REGISTRY_PROJECT_OPTION_ALERT)) {
			return Mono.error(RegistryException(status = FORBIDDEN, code = NOT_ENOUGH_PERMISSION))
		}

		return alertPort.findById(communication.projectId!!, alertId, isVisible = null)
			.switchIfEmpty(Mono.defer { alertNotFound() })
			.handle { alert, sink -> sink.nextOrError(communicationToReturn, alertConflict(alert, communication)) }
	}

	private fun alertNotFound(): Mono<AlertModel> =
		Mono.error(RegistryException(NOT_FOUND, COMMUNICATION_ALERT_NOT_FOUND_IN_COMMUNICATION_PROJECT))

	private fun alertConflict(alert: AlertModel, communication: CommunicationModel): RegistryException? = when {
		alert.isNotVisible() -> RegistryException(NOT_FOUND, COMMUNICATION_ALERT_NOT_VISIBLE)

		alert.dateTime.isAfter(communication.dateTime) ->
			RegistryException(UNPROCESSABLE_CONTENT, COMMUNICATION_ALERT_IS_AFTER_COMMUNICATION)

		alert.status !== AlertStatusEnum.IN_PROGRESS ->
			RegistryException(UNPROCESSABLE_CONTENT, COMMUNICATION_ALERT_IS_NOT_COMPATIBLE_WITH_COMMUNICATION_CREATION)

		else -> null
	}
}
