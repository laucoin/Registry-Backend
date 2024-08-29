package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.EventModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IEventModelRepository: IGenericReadModelRepository<EventModel>, IGenericWriteModelRepository<EventModel> {
    fun findAll(onlyVisible: Boolean, startDateTime: ZonedDateTime?, endDateTime: ZonedDateTime?): Flux<EventModel>
    fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean>
}
