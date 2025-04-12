package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventService {
    fun findEventsPage(
        currentUser: CurrentUserModel,
        pageable: PageableModel,
        searchParams: EventSearchParamModel,
    ): Mono<PageModel<EventModel>>

    fun findEventById(id: UUID, visibilitySearched: Boolean?): Mono<EventModel>
    fun availableEventOptions(): Flux<Pair<EventOptionEnum, Collection<EventOptionEnum>>>
    fun validateDateTime(id: UUID, dateTime: CustomDateTimeModel?, errorCode: String): Mono<UUID>
    fun validateDateTimes(id: UUID, start: CustomDateTimeModel?, end: CustomDateTimeModel?, errorCode: String): Mono<UUID>
    fun createEvent(currentUser: CurrentUserModel, event: EventModel): Mono<EventModel>
    fun updateEventById(currentUser: CurrentUserModel, id: UUID, event: EventModel): Mono<EventModel>
    fun disableEventById(currentUser: CurrentUserModel, id: UUID): Mono<EventModel>
    fun enableEventById(currentUser: CurrentUserModel, id: UUID): Mono<EventModel>
    fun deleteEventById(id: UUID): Mono<Void>
}
