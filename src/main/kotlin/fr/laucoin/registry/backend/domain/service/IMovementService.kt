package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.MovementReasonEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.CurrentUserModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import java.util.UUID
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

interface IMovementService {
    fun findMovementsPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>>

    fun findMovementsContent(
        eventId: UUID,
        movementIds: List<UUID>,
    ): Flux<Pair<UUID, List<MovementContentModel>>>

    fun findMovementById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementModel>
    fun searchParticipantsAndGroups(
        eventId: UUID,
        typeSearched: ParticipantTypeEnum,
        textSearched: String?
    ): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>>

    fun searchVehicles(eventId: UUID, textSearched: String?): Flux<VehicleModel>
    fun searchReasons(
        contentTypeSearched: ParticipantTypeEnum,
        typeSearched: MovementTypeEnum,
    ): Flux<MovementReasonEnum>

    fun searchActivities(
        eventId: UUID,
        contentTypeSearched: ParticipantTypeEnum,
        textSearched: String?
    ): Flux<ActivityModel>

    fun createMovement(
        currentUser: CurrentUserModel,
        movement: MovementModel,
        newGuests: List<ParticipantModel> = emptyList()
    ): Mono<MovementModel>

    fun updateMovementById(
        currentUser: CurrentUserModel,
        eventId: UUID,
        id: UUID,
        movement: MovementModel,
        newGuests: List<ParticipantModel> = emptyList()
    ): Mono<MovementModel>

    fun disableMovementById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<MovementModel>
    fun enableMovementById(currentUser: CurrentUserModel, eventId: UUID, id: UUID): Mono<MovementModel>
    fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void>
}
