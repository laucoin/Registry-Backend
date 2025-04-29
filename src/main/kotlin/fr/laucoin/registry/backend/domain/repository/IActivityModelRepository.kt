package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IActivityModelRepository: IGenericReadProjectModelRepository<ActivityModel>,
                                    IGenericWriteModelRepository<ActivityModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: ActivitySearchParamModel,
    ): Mono<PageModel<ActivityModel>>

    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityModel>

    fun findWithLimit(limit: Int, projectId: UUID, searchParams: ActivitySearchParamModel): Flux<ActivityModel>
}
