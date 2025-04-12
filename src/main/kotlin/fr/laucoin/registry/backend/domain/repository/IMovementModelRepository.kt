package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IMovementModelRepository: IGenericReadEventModelRepository<MovementModel>, IGenericWriteModelRepository<MovementModel> {
    fun findPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findContent(
        eventId: UUID,
        movementIds: List<UUID>,
    ): Flux<Pair<UUID, List<MovementContentModel>>>

    fun findPageByParticipantId(
        eventId: UUID,
        participantId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findPageByVehicleId(
        eventId: UUID,
        vehicleId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findPageByActivityId(
        eventId: UUID,
        activityId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun countAllByParticipantId(eventId: UUID, participantId: UUID): Mono<Long>

    fun countAllByVehicleId(eventId: UUID, vehicleId: UUID): Mono<Long>

    fun countAllByActivityId(eventId: UUID, activityId: UUID): Mono<Long>
}
