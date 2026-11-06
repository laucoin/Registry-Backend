package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.helper.CommunicationConflictHelper
import fr.laucoin.registry.backend.domain.constant.ErrorConst.CommunicationError.COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.COMMUNICATION
import fr.laucoin.registry.backend.domain.helper.ProjectDateHelper
import fr.laucoin.registry.backend.domain.helper.PurgeHelper.purgeEach
import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.port.ICommunicationHistoryPort
import fr.laucoin.registry.backend.domain.port.ICommunicationPort
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.ICommunicationService
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Gère les communications rattachées à une alerte.
 * Périmètre : Création et lecture des communications et de leurs liens avec les alertes.
 * Limites : Ne porte pas les règles de statut des alertes ; elles relèvent de AlertService.
 */
@Service
class CommunicationService(
	private val projectDates: ProjectDateHelper,
	private val port: ICommunicationPort,
	private val historyPort: ICommunicationHistoryPort,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
	private val conflicts: CommunicationConflictHelper,
	clock: Clock,
): ICommunicationService, GenericService(clock) {
	override fun findCommunicationById(
		projectId: UUID,
		id: UUID,
		isVisible: Boolean?
	): Mono<CommunicationModel> {
		return port.findById(projectId, id, isVisible).notFoundIfEmpty(id)
	}

	override fun createCommunication(
		currentUser: CurrentUserModel,
		communication: CommunicationModel
	): Mono<CommunicationModel> {
		return projectDates.validateDateTime(
			communication.projectId!!,
			CustomDateTimeModel(communication.dateTime),
			COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { conflicts.validateNoMovementConflict(communication) }
			.flatMap { conflicts.validateNoAlertConflict(currentUser, communication) }
			.flatMap { port.create(communication.apply { create(currentUser, now()) }) }
	}

	override fun updateCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		communication: CommunicationModel
	): Mono<CommunicationModel> {
		return validateCommunicationDate(communication)
			.flatMap { rowLockPort.lock(COMMUNICATION, id) }
			.flatMap { findCommunicationById(projectId, id, isVisible = null) }
			.flatMap { conflicts.validateNoMovementConflict(communication, it) }
			.flatMap { conflicts.validateNoAlertConflict(currentUser, communication, it) }
			.map { it.withContentOf(communication) }
			.archiveCurrentState()
			.updateCommunication(currentUser)
			.`as`(transactionalOperator::transactional)
	}

	private fun validateCommunicationDate(communication: CommunicationModel): Mono<UUID> {
		return projectDates.validateDateTime(
			communication.projectId!!,
			CustomDateTimeModel(communication.dateTime),
			COMMUNICATION_DATETIME_OUT_OF_PROJECT_DATE_RANGE,
		)
	}

	private fun Mono<CommunicationModel>.archiveCurrentState(): Mono<CommunicationModel> = flatMap {
		historyPort.archiveCommunication(it.id!!).thenReturn(it)
	}

	override fun disableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<CommunicationModel> {
		return findCommunicationById(projectId, id, isVisible = true)
			.updateVisibility(visibility = false)
			.updateCommunication(currentUser)
	}

	override fun enableCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<CommunicationModel> {
		return findCommunicationById(projectId, id, isVisible = false)
			.updateVisibility(visibility = true)
			.updateCommunication(currentUser)
	}

	override fun deleteCommunicationById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID
	): Mono<Unit> {
		return findCommunicationById(projectId, id, isVisible = null)
			.flatMap { port.deleteById(it.id!!) }
	}

	override fun purgeOrphanCommunications(
		movementsToExclude: List<UUID>,
		alertsToExclude: List<UUID>,
		dryRun: Boolean
	): Flux<UUID> {
		log.info("Purging orphan communications")
		return port.findOrphan(movementsToExclude, alertsToExclude)
			.purgeEach("communication", dryRun, port::deleteById)
	}

	private fun Mono<CommunicationModel>.updateCommunication(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser, now()) })
	}

	private fun CommunicationModel.withContentOf(communication: CommunicationModel): CommunicationModel = apply {
		dateTime = communication.dateTime
		movement = communication.movement
		message = communication.message
	}
}
