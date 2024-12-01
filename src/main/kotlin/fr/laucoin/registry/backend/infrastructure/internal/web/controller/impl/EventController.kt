package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IEventController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.EventDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.EventDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class EventController(
    private val service: IEventService,
    private val mapper: EventDtoMapper,
): IEventController {
    override fun findEvents(
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageModel<EventModel>> {
        return currentUser().flatMapMany {
            service.findEvents(it, order, onlyVisible, searched, startDateTime, endDateTime)
        }.paginate(offset, limit)
    }

    override fun findEventById(id: UUID): Mono<EventModel> {
        return currentUser().flatMap { service.findEventById(id, onlyVisible = false) }
    }

    override fun createEvent(event: EventDto): Mono<EventModel> {
        return currentUser().flatMap { service.createEvent(it, mapper.toModel(event)) }
    }

    override fun updateEventById(id: UUID, event: EventDto): Mono<EventModel> {
        return currentUser().flatMap { service.updateEventById(it, id, mapper.toModel(event)) }
    }

    override fun disableEventById(id: UUID): Mono<EventModel> {
        return currentUser().flatMap { service.disableEventById(it, id) }
    }

    override fun enableEventById(id: UUID): Mono<EventModel> {
        return currentUser().flatMap { service.enableEventById(it, id) }
    }

    override fun deleteEventById(id: UUID): Mono<Void> = service.deleteEventById(id)
}
