package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IVehicleModelRepository: IGenericReadProjectModelRepository<VehicleModel>,
                                   IGenericWriteModelRepository<VehicleModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: VehicleSearchParamModel,
    ): Mono<PageModel<VehicleModel>>

    fun countAll(projectId: UUID, searchParams: VehicleSearchParamModel): Mono<Long>

    fun findAllByIds(projectId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<VehicleModel>

    fun findWithLimit(limit: Int, projectId: UUID, searchParams: VehicleSearchParamModel): Flux<VehicleModel>

    fun findUnusedSince(dateThreshold: LocalDate): Flux<UUID>
}
