package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ProjectModel
import fr.laucoin.registry.backend.domain.model.ProjectSearchParamModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Mono

interface IProjectModelRepository: IGenericReadModelRepository<ProjectModel>, IGenericWriteModelRepository<ProjectModel> {
    fun findPage(
        pageable: PageableModel,
        searchParams: ProjectSearchParamModel,
    ): Mono<PageModel<ProjectModel>>

    fun findPage(
        projectIds: List<UUID>,
        pageable: PageableModel,
        searchParams: ProjectSearchParamModel,
    ): Mono<PageModel<ProjectModel>>

    fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean>
}
