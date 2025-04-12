package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IActivityModelRepository: IGenericReadEventModelRepository<ActivityModel>,
                                    IGenericWriteModelRepository<ActivityModel> {
    fun findPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: ActivitySearchParamModel,
    ): Mono<PageModel<ActivityModel>>

    fun findAllByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityModel>

    fun findWithLimit(limit: Int, eventId: UUID, searchParams: ActivitySearchParamModel): Flux<ActivityModel>
}
