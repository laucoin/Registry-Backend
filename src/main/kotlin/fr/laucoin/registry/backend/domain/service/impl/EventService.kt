package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.EventError.EVENT_DATE_CONFLICT_WITH_ELEMENTS
import fr.laucoin.registry.backend.domain.extension.DateExt.notInRange
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.RegistryExceptionModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IRoleService
import fr.laucoin.registry.backend.domain.service.IUserEventProfileService
import java.time.ZonedDateTime
import java.util.Objects
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
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
): IEventService, GenericService<EventModel>(compareBy { it.name }) {
    override fun findEvents(
        currentUser: CurrentUserModel,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<EventModel> {
        val userEventIds: List<UUID> = roleService.getEventIdFromCurrentUserProfiles(currentUser)
        val eventReader: Boolean = roleService.getAuthoritiesByUserRole(currentUser.role).contains("REGISTRY_EVENT_R")
        return repository.findAll(onlyVisible, startDateTime, endDateTime)
            .filter { eventReader || userEventIds.contains(it.id) }
            .searchAndSort(order, searched)
    }

    override fun findEventById(id: UUID, onlyVisible: Boolean): Mono<EventModel> {
        return repository.findById(id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun validateDateTime(id: UUID, dateTime: ZonedDateTime?, message: String): Mono<UUID> {
        return findEventById(id, onlyVisible = false)
            .handle { it, handle ->
                if (dateTime.notInRange(it.begin, it.end)) {
                    log.warn("Failed to editing, date {} is out of event range [{}, {}]", dateTime, it.begin, it.end)
                    handle.error(
                        RegistryExceptionModel(
                            status = CONFLICT,
                            message = message,
                            args = mapOf(
                                "begin" to it.begin.toString(),
                                "end" to it.end.toString(),
                                "actual" to dateTime.toString(),
                            )
                        )
                    )
                } else handle.next(id)
            }
    }

    override fun validateDateTimes(id: UUID, start: ZonedDateTime?, end: ZonedDateTime?, message: String): Mono<UUID> {
        return findEventById(id, onlyVisible = false)
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
                        RegistryExceptionModel(
                            status = CONFLICT,
                            message = message,
                            args = mapOf(
                                "begin" to it.begin.toString(),
                                "end" to it.end.toString(),
                                "actualBegin" to start.toString(),
                                "actualEnd" to end.toString(),
                            )
                        )
                    )
                } else handle.next(id)
            }
    }

    override fun createEvent(currentUser: UserModel, event: EventModel): Mono<EventModel> {
        return repository.save(event.apply { create(currentUser) })
            .flatMap {
                userEventProfileService.createUserEventProfileFromEvent(currentUser, it)
                    .thenReturn(it)
            }
            .`as`(transactionalOperator::transactional)
    }

    override fun updateEventById(currentUser: UserModel, id: UUID, event: EventModel): Mono<EventModel> {
        return findEventById(id, onlyVisible = false)
            .validateBeginDate(event)
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

    private fun Mono<EventModel>.updateEvent(currentUser: UserModel) = flatMap {
        repository.save(it.apply { update(currentUser) })
    }

    private fun Mono<EventModel>.validateBeginDate(event: EventModel): Mono<EventModel> = flatMap {
        if (
            (Objects.nonNull(event.begin) && (Objects.isNull(it.begin) || it.begin !!.isBefore(event.begin)))
            || (Objects.nonNull(event.end) && (Objects.isNull(it.end) || it.end !!.isAfter(event.end)))
        ) {
            repository.validDateTime(it.id !!, event.begin, event.end)
                .handle { valid, handle ->
                    if (! valid) {
                        log.warn("Failed, {} is out of event range [{}, {}]", it, it.begin, it.end)
                        handle.error(RegistryExceptionModel(CONFLICT, EVENT_DATE_CONFLICT_WITH_ELEMENTS))
                    } else handle.next(it)
                }
        } else Mono.just(it)
    }

    override fun disableEventById(currentUser: UserModel, id: UUID): Mono<EventModel> {
        return findEventById(id, onlyVisible = true)
            .updateVisibility(visibility = false)
            .updateEvent(currentUser)
    }

    override fun enableEventById(currentUser: UserModel, id: UUID): Mono<EventModel> {
        return findEventById(id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .updateEvent(currentUser)
    }

    override fun deleteEventById(id: UUID): Mono<Void> {
        return findEventById(id, onlyVisible = false)
            .flatMap { repository.deleteById(id) }
    }
}
