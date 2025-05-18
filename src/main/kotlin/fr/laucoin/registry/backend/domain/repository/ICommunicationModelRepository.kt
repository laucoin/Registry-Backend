package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ICommunicationModelRepository: IGenericReadProjectModelRepository<CommunicationModel>,
                                         IGenericWriteModelRepository<CommunicationModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel,
    ): Mono<PageModel<CommunicationModel>>

    fun findByMovementIdsWithLimit(
        limit: Int,
        projectId: UUID,
        movementIds: List<UUID>,
        visibilitySearched: Boolean?,
    ): Flux<Pair<UUID, List<CommunicationModel>>>

    fun findPageByMovementId(
        projectId: UUID,
        movementId: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel
    ): Mono<PageModel<CommunicationModel>>

    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<CommunicationModel>

    fun countAllByMovementId(projectId: UUID, movementId: UUID, searchParams: CommunicationSearchParamModel): Mono<Long>
}
