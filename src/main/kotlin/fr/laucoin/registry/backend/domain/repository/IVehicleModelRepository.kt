package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.model.VehicleSearchParamModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IVehicleModelRepository: IGenericReadEventModelRepository<VehicleModel>,
                                   IGenericWriteModelRepository<VehicleModel> {
    fun findPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: VehicleSearchParamModel,
    ): Mono<PageModel<VehicleModel>>

    fun findAllByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<VehicleModel>

    fun findWithLimit(limit: Int, eventId: UUID, searchParams: VehicleSearchParamModel): Flux<VehicleModel>
}
