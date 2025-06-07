package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import java.time.LocalDate
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface IMovementModelRepository: IGenericReadProjectModelRepository<MovementModel>, IGenericWriteModelRepository<MovementModel> {
    fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findCurrentPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findContent(
        projectId: UUID,
        movementIds: List<UUID>,
    ): Flux<Pair<UUID, List<MovementContentModel>>>

    fun findCurrentContent(
        projectId: UUID,
        movementIds: List<UUID>,
    ): Flux<Pair<UUID, List<MovementContentModel>>>

    fun findPageByParticipantId(
        projectId: UUID,
        participantId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findPageByVehicleId(
        projectId: UUID,
        vehicleId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findPageByActivityId(
        projectId: UUID,
        activityId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findActivityWithLimit(
        limit: Int,
        projectId: UUID,
        searchParams: ActivitySearchParamModel,
    ): Flux<MovementModel>

    fun countAllByParticipantId(projectId: UUID, participantId: UUID, searchParams: MovementSearchParamModel): Mono<Long>

    fun countAllByVehicleId(projectId: UUID, vehicleId: UUID, searchParams: MovementSearchParamModel): Mono<Long>

    fun countAllByActivityId(projectId: UUID, activityId: UUID, searchParams: MovementSearchParamModel): Mono<Long>

    fun findOlderThanAndUncommentedSince(dateThreshold: LocalDate): Flux<UUID>
}
