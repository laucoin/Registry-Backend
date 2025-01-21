package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IEventController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.PageDto.Companion.paginate
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventWriterDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class EventController(
    private val service: IEventService,
    private val mapper: EventWriterDtoMapper,
): IEventController {
    override fun findEvents(
        currentUser: CurrentUserModel,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageDto<EventModel>> {
        return service.findEvents(currentUser, order, onlyVisible, searched, startDateTime, endDateTime)
            .paginate(offset, limit)
    }

    override fun findEventById(id: UUID): Mono<EventModel> {
        return service.findEventById(id, onlyVisible = false)
    }

    override fun createEvent(currentUser: CurrentUserModel, event: EventWriterDto): Mono<EventModel> {
        return service.createEvent(currentUser, mapper.toModel(event))
    }

    override fun updateEventById(currentUser: CurrentUserModel, id: UUID, event: EventWriterDto): Mono<EventModel> {
        return service.updateEventById(currentUser, id, mapper.toModel(event))
    }

    override fun disableEventById(currentUser: CurrentUserModel, id: UUID): Mono<EventModel> {
        return service.disableEventById(currentUser, id)
    }

    override fun enableEventById(currentUser: CurrentUserModel, id: UUID): Mono<EventModel> {
        return service.enableEventById(currentUser, id)
    }

    override fun deleteEventById(id: UUID): Mono<Void> = service.deleteEventById(id)
}
