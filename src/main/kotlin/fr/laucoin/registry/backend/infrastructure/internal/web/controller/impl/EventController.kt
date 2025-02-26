package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IEventController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventOptionsReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader.EventReaderDto
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.writer.EventWriterDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventOptionsReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.reader.EventReaderDtoMapper
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.writer.EventWriterDtoMapper
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class EventController(
    private val service: IEventService,
    private val readerMapper: EventReaderDtoMapper,
    private val optionsReaderMapper: EventOptionsReaderDtoMapper,
    private val writerMapper: EventWriterDtoMapper,
): IEventController {
    override fun findEvents(
        currentUser: CurrentUserModel,
        locale: Locale,
        pageNumber: Int,
        pageSize: Int,
        textSearched: String?,
        visibilitySearched: Boolean?,
        dateTimeSearched: ZonedDateTime?,
    ): Mono<PageModel<EventReaderDto>> {
        return service.findEventsPage(
            currentUser,
            PageableModel(pageNumber * pageSize, pageSize),
            EventSearchParamModel(textSearched, visibilitySearched, dateTimeSearched),
        ).map { readerMapper.toDtoPage(it, locale) }
    }

    override fun findEventById(locale: Locale, id: UUID): Mono<EventReaderDto> {
        return service.findEventById(id, visibilitySearched = null)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun getAvailableEventOptions(locale: Locale): Flux<EventOptionsReaderDto> {
        return service.availableEventOptions()
            .map { optionsReaderMapper.toDto(it, locale) }
    }

    override fun createEvent(currentUser: CurrentUserModel, locale: Locale, event: EventWriterDto): Mono<EventReaderDto> {
        return service.createEvent(currentUser, writerMapper.toModel(event))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun updateEventById(
        currentUser: CurrentUserModel,
        locale: Locale,
        id: UUID,
        event: EventWriterDto
    ): Mono<EventReaderDto> {
        return service.updateEventById(currentUser, id, writerMapper.toModel(event))
            .map { readerMapper.toDto(it, locale) }
    }

    override fun disableEventById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<EventReaderDto> {
        return service.disableEventById(currentUser, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun enableEventById(currentUser: CurrentUserModel, locale: Locale, id: UUID): Mono<EventReaderDto> {
        return service.enableEventById(currentUser, id)
            .map { readerMapper.toDto(it, locale) }
    }

    override fun deleteEventById(id: UUID): Mono<Void> = service.deleteEventById(id)
}
