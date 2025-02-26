package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDateTime
import java.util.UUID
import reactor.core.publisher.Mono

interface IEventModelRepository: IGenericReadModelRepository<EventModel>, IGenericWriteModelRepository<EventModel> {
    fun findPage(
        pageable: PageableModel,
        searchParams: EventSearchParamModel,
    ): Mono<PageModel<EventModel>>

    fun findPage(
        eventIds: List<UUID>,
        pageable: PageableModel,
        searchParams: EventSearchParamModel,
    ): Mono<PageModel<EventModel>>

    fun validDateTime(id: UUID, begin: LocalDateTime?, end: LocalDateTime?): Mono<Boolean>
}
