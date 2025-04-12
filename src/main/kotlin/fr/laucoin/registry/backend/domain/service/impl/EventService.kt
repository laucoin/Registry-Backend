package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.constant.EventOptionsConst.optionsRules
import fr.laucoin.registry.backend.domain.constant.EventPermissionConst.REGISTRY_EVENT_R
import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.extension.DateExt.isAfter
import fr.laucoin.registry.backend.domain.extension.DateExt.isBefore
import fr.laucoin.registry.backend.domain.extension.DateExt.notInRange
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.time.LocalTime
import java.util.UUID
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventService(
    private val repository: IEventModelRepository,
    private val userEventProfileService: IUserEventProfileService,
    private val transactionalOperator: TransactionalOperator,
    private val roleService: IRoleService,
): IEventService, GenericService() {
    override fun findEventsPage(
        currentUser: CurrentUserModel,
        pageable: PageableModel,
        searchParams: EventSearchParamModel,
    ): Mono<PageModel<EventModel>> {
        return if (roleService.getAuthoritiesByUserRole(currentUser.role).contains(REGISTRY_EVENT_R)) {
            repository.findPage(pageable, searchParams)
        } else repository.findPage(roleService.getEventIdsFromCurrentUserProfiles(currentUser), pageable, searchParams)
    }

    override fun findEventById(id: UUID, visibilitySearched: Boolean?): Mono<EventModel> {
        return repository.findById(id, visibilitySearched)
            .notFoundIfEmpty(id)
    }

    override fun availableEventOptions(): Flux<Pair<EventOptionEnum, Collection<EventOptionEnum>>> {
        return Flux.fromIterable(optionsRules.map { Pair(it.key, it.value) })
    }

    override fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
        return findEventById(id, visibilitySearched = null)
            .handle { it, handle ->
                if (dateTime.notInRange(it.begin, it.end)) {
                    log.warn("Failed to editing, date {} is out of event range [{}, {}]", dateTime, it.begin, it.end)
                    handle.error(
                        RegistryException(
                            status = CONFLICT,
                            code = errorCode,
                            args = arrayListOf(dateTime.toString(), it.begin.toString(), it.end.toString()),
                        )
                    )
                } else handle.next(id)
            }
    }

    override fun validateDateTimes(id: UUID, start: CustomDateTimeModel?, end: CustomDateTimeModel?, errorCode: String): Mono<UUID> {
        return findEventById(id, visibilitySearched = null)
            .handle { it, handle ->
                if (start.notInRange(it.begin, it.end) || end.notInRange(it.begin, it.end)) {
                    log.warn(
                        "Failed to editing, one or more dates ({}, {}) are out of event range [{}, {}]",
                        start,
                        end,
                        it.begin,
                        it.end
                    )
                    handle.error(
                        RegistryException(
                            status = CONFLICT,
                            code = errorCode,
                            args = arrayListOf(start.toString(), end.toString(), it.begin.toString(), it.end.toString()),
                        )
                    )
                } else handle.next(id)
            }
    }

    override fun createEvent(currentUser: CurrentUserModel, event: EventModel): Mono<EventModel> {
        return repository.create(event.apply { create(currentUser) })
            .flatMap {
                userEventProfileService.createUserEventProfileFromEvent(currentUser, it)
                    .thenReturn(it)
            }
            .`as`(transactionalOperator::transactional)
    }

    override fun updateEventById(currentUser: CurrentUserModel, id: UUID, event: EventModel): Mono<EventModel> {
        return findEventById(id, visibilitySearched = null)
            .validateDates(event)
            .map {
                it.apply {
                    name = event.name
                    begin = event.begin
                    end = event.end
                    options = event.options
                }
            }
            .updateEvent(currentUser)
    }

    private fun Mono<EventModel>.updateEvent(currentUser: CurrentUserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun Mono<EventModel>.validateDates(event: EventModel): Mono<EventModel> = flatMap {
        if (it.begin.isBefore(event.begin) || it.end.isAfter(event.end)) {
            repository.validDateTime(it.id !!, event.begin?.toLocalDateTime(LocalTime.MIN), event.end?.toLocalDateTime(LocalTime.MAX))
                .handle { valid, handle ->
                    if (! valid) {
                        log.warn("Failed, {} is out of event range [{}, {}]", it, it.begin, it.end)
                        handle.error(RegistryException(CONFLICT, EVENT_DATE_CONFLICT_WITH_ELEMENTS))
                    } else handle.next(it)
                }
        } else Mono.just(it)
    }

    override fun disableEventById(currentUser: CurrentUserModel, id: UUID): Mono<EventModel> {
        return findEventById(id, visibilitySearched = true)
            .updateVisibility(visibility = false)
            .updateEvent(currentUser)
    }

    override fun enableEventById(currentUser: CurrentUserModel, id: UUID): Mono<EventModel> {
        return findEventById(id, visibilitySearched = false)
            .updateVisibility(visibility = true)
            .updateEvent(currentUser)
    }

    override fun deleteEventById(id: UUID): Mono<Void> {
        return findEventById(id, visibilitySearched = null)
            .flatMap { repository.deleteById(id) }
    }
}
