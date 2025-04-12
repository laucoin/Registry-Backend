package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_DELETE_HAS_MOVEMENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.ActivityError.ACTIVITY_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IActivityService
import fr.laucoin.registry.backend.domain.service.IEventService
import java.util.UUID
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ActivityService(
    private val eventService: IEventService,
    private val repository: IActivityModelRepository,
    private val movementRepository: IMovementModelRepository,
): IActivityService, GenericService() {
    override fun findActivitiesPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: ActivitySearchParamModel,
    ): Mono<PageModel<ActivityModel>> {
        return repository.findPage(eventId, pageable, searchParams)
    }

    override fun findActivityById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel> {
        return repository.findById(eventId, id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun findActivityMovementsPage(
        eventId: UUID,
        id: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel
    ): Mono<PageModel<MovementModel>> {
        return movementRepository.findPageByActivityId(eventId, id, pageable, searchParams)
    }

    override fun createActivity(currentUser: CurrentUserModel, activity: ActivityModel): Mono<ActivityModel> {
        return eventService.validateDateTimes(
            activity.event !!.id !!,
            activity.startAvailability,
            activity.endAvailability,
            ACTIVITY_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { repository.create(activity.apply { create(currentUser) }) }
    }

    override fun updateActivityById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        activity: ActivityModel
    ): Mono<ActivityModel> {
        return eventService.validateDateTimes(
            activity.event !!.id !!,
            activity.startAvailability,
            activity.endAvailability,
            ACTIVITY_PRESENCE_DATES_OUT_OF_EVENT_DATE_RANGE,
        )
            .flatMap { findActivityById(eventId, id, visibilitySearched = null) }
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

    override fun disableActivityById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ActivityModel> {
        return findActivityById(eventId, id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateActivity(currentUser)
    }

    override fun enableActivityById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<ActivityModel> {
        return findActivityById(eventId, id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateActivity(currentUser)
    }

    override fun deleteActivityById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<Void> {
        return findActivityById(eventId, id, visibilitySearched = null)
            .validateHasNoMovementLinked(ACTIVITY_DELETE_HAS_MOVEMENT)
            .flatMap { repository.deleteById(it.id !!) }
    }

    private fun Mono<ActivityModel>.updateActivity(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun Mono<ActivityModel>.validateHasNoMovementLinked(error: String) = flatMap { activityToUpdate ->
        movementRepository.countAllByActivityId(
            activityToUpdate.event !!.id !!,
            activityToUpdate.id !!,
        ).handle { it, handle ->
            if (it > 0) {
                log.warn("The activity {} already linked to movement(s)", activityToUpdate.id)
                handle.error(RegistryException(FORBIDDEN, error))
            } else handle.next(activityToUpdate)
        }
    }
}
