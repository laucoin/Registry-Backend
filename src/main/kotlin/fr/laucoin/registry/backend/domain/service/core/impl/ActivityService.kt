package fr.laucoin.registry.backend.domain.service.core.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.nextOrError
import fr.laucoin.registry.backend.domain.enumeration.LockableResourceEnum.ACTIVITY
import fr.laucoin.registry.backend.domain.port.IRowLockPort
import org.springframework.transaction.reactive.TransactionalOperator
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.helper.ProjectDateHelper
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivityStatisticsModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.port.IActivityPort
import fr.laucoin.registry.backend.domain.port.IMovementPort
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.core.IActivityService
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Objectif : Construit le fil d'activité d'un projet à partir de ses mouvements, groupes et participants.
 * Périmètre : Lit et agrège les entrées d'activité via les ports.
 * Limites : N'écrit aucune donnée et ne vérifie pas les permissions (fait sur le contrat).
 */
@Service
class ActivityService(
	private val projectDates: ProjectDateHelper,
	private val port: IActivityPort,
	private val movementPort: IMovementPort,
	private val rowLockPort: IRowLockPort,
	private val transactionalOperator: TransactionalOperator,
	clock: Clock,
): IActivityService, GenericService(clock) {
	override fun findActivitiesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
	): Mono<PageModel<ActivityModel>> {
		return port.findPage(projectId, pageable, searchParams)
	}

	override fun findActivityById(projectId: UUID, id: UUID, isVisible: Boolean?): Mono<ActivityModel> {
		return port.findById(projectId, id, isVisible).notFoundIfEmpty(id)
	}

	override fun findActivityStatistics(projectId: UUID, id: UUID): Mono<ActivityStatisticsModel> {
		return findActivityById(projectId, id, isVisible = null)
			.flatMap { movementPort.findStatisticsByActivityId(projectId, id) }
	}

	override fun findActivityMovementsPage(
		projectId: UUID,
		id: UUID,
		pageable: PageableModel,
		searchParams: MovementSearchParamModel
	): Mono<PageModel<MovementModel>> {
		return movementPort.findPageByActivityId(projectId, id, pageable, searchParams)
	}

	override fun createActivity(currentUser: CurrentUserModel, activity: ActivityModel): Mono<ActivityModel> {
		return projectDates.validateDateTimes(
			activity.projectId!!,
			activity.startAvailability,
			activity.endAvailability,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		).flatMap { port.create(activity.apply { create(currentUser, now()) }) }
	}

	override fun updateActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		activity: ActivityModel
	): Mono<ActivityModel> {
		return projectDates.validateDateTimes(
			activity.projectId!!,
			activity.startAvailability,
			activity.endAvailability,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { findActivityById(projectId, id, isVisible = null) }
			.map { it.withContentOf(activity) }
			.updateActivity(currentUser)
	}

	override fun disableActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityModel> {
		return findActivityById(projectId, id, isVisible = true)
			.updateVisibility(visibility = false)
			.updateActivity(currentUser)
	}

	override fun enableActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityModel> {
		return findActivityById(projectId, id, isVisible = false)
			.updateVisibility(visibility = true)
			.updateActivity(currentUser)
	}

	override fun deleteActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Unit> {
		return findActivityById(projectId, id, isVisible = null)
			.flatMap { rowLockPort.lock(ACTIVITY, id).thenReturn(it) }
			.validateHasNoMovementLinked(ACTIVITY_DELETE_HAS_MOVEMENT)
			.flatMap { port.deleteById(it.id!!) }
			.`as`(transactionalOperator::transactional)
	}

	override fun purgeActivitiesIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging activities unused since {}", dateThreshold)
		return port.findUnusedSince(dateThreshold)
			.purgeEach("activity", dryRun, port::deleteById)
	}

	private fun Mono<ActivityModel>.updateActivity(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser, now()) })
	}

	private fun Mono<ActivityModel>.validateHasNoMovementLinked(error: String): Mono<ActivityModel> = flatMap { rejectIfLinkedToMovement(it, error) }

	private fun rejectIfLinkedToMovement(activity: ActivityModel, error: String): Mono<ActivityModel> {
		return movementPort.countAllByActivityId(activity.projectId!!, activity.id!!, MovementSearchParamModel())
			.handle { count, sink -> sink.nextOrError(activity, linkedToMovementError(activity, count, error)) }
	}

	private fun linkedToMovementError(activity: ActivityModel, count: Long, error: String): RegistryException? {
		if (count <= 0) return null
		log.warn("The activity {} already linked to movement(s)", activity.id)
		return RegistryException(UNPROCESSABLE_CONTENT, error)
	}

	private fun ActivityModel.withContentOf(activity: ActivityModel): ActivityModel = apply {
		name = activity.name
		description = activity.description
		duration = activity.duration
		allowedParticipants = activity.allowedParticipants
		startAvailability = activity.startAvailability
		endAvailability = activity.endAvailability
	}
}
