package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.GroupSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IGroupModelRepository: IGenericReadProjectModelRepository<GroupModel>, IGenericWriteModelRepository<GroupModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: GroupSearchParamModel,
    ): Mono<PageModel<GroupModel>>

    fun findContent(
        projectId: UUID,
        groupIds: List<UUID>,
    ): Flux<Pair<UUID, List<ParticipantModel>>>

    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<GroupModel>

    fun findWithLimit(limit: Int, projectId: UUID, searchParams: GroupSearchParamModel): Flux<GroupModel>
}
