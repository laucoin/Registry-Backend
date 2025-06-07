package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.AlertModel
import fr.laucoin.registry.backend.domain.model.AlertSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IAlertModelRepository: IGenericReadProjectModelRepository<AlertModel>,
                                 IGenericWriteModelRepository<AlertModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: AlertSearchParamModel,
    ): Mono<PageModel<AlertModel>>

    fun findWithLimit(limit: Int, projectId: UUID, searchParams: AlertSearchParamModel): Flux<AlertModel>

    fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID>
}
