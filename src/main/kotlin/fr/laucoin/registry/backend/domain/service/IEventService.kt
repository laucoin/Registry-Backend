package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.EventOptionEnum
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventService {
    fun findEvents(
        currentUser: CurrentUserModel,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<EventModel>

    fun findEventById(id: UUID, onlyVisible: Boolean): Mono<EventModel>
    fun availableEventOptions(): Flux<Pair<EventOptionEnum, Collection<EventOptionEnum>>>
    fun validateDateTime(id: UUID, dateTime: ZonedDateTime?, errorCode: String): Mono<UUID>
    fun validateDateTimes(id: UUID, start: ZonedDateTime?, end: ZonedDateTime?, errorCode: String): Mono<UUID>
    fun createEvent(currentUser: UserModel, event: EventModel): Mono<EventModel>
    fun updateEventById(currentUser: UserModel, id: UUID, event: EventModel): Mono<EventModel>
    fun disableEventById(currentUser: UserModel, id: UUID): Mono<EventModel>
    fun enableEventById(currentUser: UserModel, id: UUID): Mono<EventModel>
    fun deleteEventById(id: UUID): Mono<Void>
}
