package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ParticipantError.PARTICIPANT_OUT_OF_MOVEMENT_DATETIME
import fr.laucoin.registry.backend.domain.extension.DateExt.asEndIsBeforeOther
import fr.laucoin.registry.backend.domain.extension.DateExt.asStartIsAfterOther
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.service.IParticipantMovementValidationService
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

/**
 * Objectif : Valide la cohérence entre un participant et ses mouvements.
* Périmètre : Refus d'un changement de dates de présence en conflit avec des mouvements, et d'une suppression d'un
 *   participant ayant des mouvements.
 * Limites : Ne modifie rien ; ne valide ni les groupes ni l'utilisateur lié.
 */
@Service
class ParticipantMovementValidationService(
	private val movementPort: IMovementPort,
) : IParticipantMovementValidationService, LoggerService() {
	override fun validateNoMovementConflict(
		participant: ParticipantModel, oldParticipant: ParticipantModel
	): Mono<ParticipantModel> {
		val smartStartCheck = if (participant.startAvailability.asStartIsAfterOther(oldParticipant.startAvailability))
			validateNoMovementConflictBefore(participant, oldParticipant)
		else Mono.just(oldParticipant)

		val smartEndCheck = if (participant.endAvailability.asEndIsBeforeOther(oldParticipant.endAvailability))
			validateNoMovementConflictAfter(participant, oldParticipant)
		else Mono.just(oldParticipant)

		return Mono.zip(smartStartCheck, smartEndCheck).map { oldParticipant }
	}

	private fun validateNoMovementConflictBefore(
		participant: ParticipantModel, oldParticipant: ParticipantModel
	): Mono<ParticipantModel> {
		val searchParams = MovementSearchParamModel(
			isVisible = null,
			type = null,
			to = participant.startAvailability!!.toZonedDateTime(),
		)
		return rejectMovements(oldParticipant, searchParams, "before the new end date")
	}

	private fun validateNoMovementConflictAfter(
		participant: ParticipantModel, oldParticipant: ParticipantModel
	): Mono<ParticipantModel> {
		val searchParams = MovementSearchParamModel(
			isVisible = null,
			type = null,
			from = participant.endAvailability!!.toZonedDateTime(),
		)
		return rejectMovements(oldParticipant, searchParams, "after the new start date")
	}

	private fun rejectMovements(
		oldParticipant: ParticipantModel,
		searchParams: MovementSearchParamModel,
		relation: String,
	): Mono<ParticipantModel> {
		return movementPort.countAllByParticipantId(oldParticipant.projectId!!, oldParticipant.id!!, searchParams)
			.handle { count, handle ->
				if (count > 0) {
					log.warn("The participant {} already has {} movement(s) {}", oldParticipant.id, count, relation)
					handle.error(
						RegistryException(UNPROCESSABLE_CONTENT, PARTICIPANT_OUT_OF_MOVEMENT_DATETIME, arrayListOf(count))
					)
				} else handle.next(oldParticipant)
			}
	}

	override fun validateHasNoMovementLinked(
		participantToUpdate: ParticipantModel,
		error: String,
	): Mono<ParticipantModel> =
		movementPort.countAllByParticipantId(
			participantToUpdate.projectId!!,
			participantToUpdate.id!!,
			MovementSearchParamModel(),
		).handle { linkedCount, handle ->
			if (linkedCount > 0) {
				log.warn("The participant {} already linked to movement(s)", participantToUpdate.id)
				handle.error(RegistryException(CONFLICT, error))
			} else handle.next(participantToUpdate)
		}
}
