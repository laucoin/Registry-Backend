package fr.laucoin.registry.backend.domain.service

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

interface IMovementService {
    fun findMovements(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?,
    ): Flux<MovementModel>

    fun findMovementById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<MovementModel>
    fun searchParticipantsAndGroups(eventId: UUID, searched: String?): Mono<Tuple2<List<ParticipantModel>, List<GroupModel>>>
    fun availableMovementTypes(): Flux<MovementTypeEnum>
    fun createMovement(currentUser: UserModel, movement: MovementModel): Mono<MovementModel>
    fun updateMovementById(currentUser: UserModel, eventId: UUID, id: UUID, movement: MovementModel): Mono<MovementModel>
    fun disableMovementById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<MovementModel>
    fun enableMovementById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<MovementModel>
    fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void>
}
