package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ActivityModel
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
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IProjectService
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ActivityService(
	private val projectService: IProjectService,
	private val port: IActivityPort,
	private val movementPort: IMovementPort,
): IActivityService, GenericService() {
	override fun findActivitiesPage(
		projectId: UUID,
		pageable: PageableModel,
		searchParams: ActivitySearchParamModel,
	): Mono<PageModel<ActivityModel>> {
		return port.findPage(projectId, pageable, searchParams)
	}

	override fun findActivityById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel> {
		return port.findById(projectId, id, visibilitySearched).notFoundIfEmpty(id)
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
		return projectService.validateDateTimes(
			activity.project!!.id!!,
			activity.startAvailability,
			activity.endAvailability,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		).flatMap { port.create(activity.apply { create(currentUser) }) }
	}

	override fun updateActivityById(
		currentUser: CurrentUserModel,
		projectId: UUID,
		id: UUID,
		activity: ActivityModel
	): Mono<ActivityModel> {
		return projectService.validateDateTimes(
			activity.project!!.id!!,
			activity.startAvailability,
			activity.endAvailability,
			ACTIVITY_PRESENCE_DATES_OUT_OF_PROJECT_DATE_RANGE,
		)
			.flatMap { findActivityById(projectId, id, visibilitySearched = null) }
			.map {
				it.apply {
					name = activity.name
					description = activity.description
					duration = activity.duration
					allowedParticipants = activity.allowedParticipants
					startAvailability = activity.startAvailability
					endAvailability = activity.endAvailability
				}
			}
			.updateActivity(currentUser)
	}

	override fun disableActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityModel> {
		return findActivityById(projectId, id, visibilitySearched = true)
			.updateVisibility(visibility = false)
			.updateActivity(currentUser)
	}

	override fun enableActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<ActivityModel> {
		return findActivityById(projectId, id, visibilitySearched = false)
			.updateVisibility(visibility = true)
			.updateActivity(currentUser)
	}

	override fun deleteActivityById(currentUser: CurrentUserModel, projectId: UUID, id: UUID): Mono<Void> {
		return findActivityById(projectId, id, visibilitySearched = null)
			.validateHasNoMovementLinked(ACTIVITY_DELETE_HAS_MOVEMENT)
			.flatMap { port.deleteById(it.id!!) }
	}

	override fun purgeActivitiesIfNecessary(dateThreshold: LocalDate, dryRun: Boolean): Flux<UUID> {
		log.info("Purging activities unused since {}", dateThreshold)
		return port.findUnusedSince(dateThreshold)
			.flatMap {
				if (dryRun) {
					log.info("[Dry run] activity {} would be deleted", it)
					Mono.just(it)
				} else {
					log.info("Purging activity {}", it)
					port.deleteById(it).thenReturn(it)
						.doOnNext { e -> log.info("Activity {} was deleted", e) }
						.doOnError { err -> log.error("Failed to purge activity {}", it, err) }
				}
			}
	}

	private fun Mono<ActivityModel>.updateActivity(currentUser: CurrentUserModel) = flatMap {
		port.update(it.apply { update(currentUser) })
	}

	private fun Mono<ActivityModel>.validateHasNoMovementLinked(error: String) = flatMap { activityToUpdate ->
		movementPort.countAllByActivityId(
			activityToUpdate.project!!.id!!,
			activityToUpdate.id!!,
			MovementSearchParamModel(),
		).handle { it, handle ->
			if (it > 0) {
				log.warn("The activity {} already linked to movement(s)", activityToUpdate.id)
				handle.error(RegistryException(UNPROCESSABLE_ENTITY, error))
			} else handle.next(activityToUpdate)
		}
	}
}
