package fr.laucoin.registry.backend.infrastructure.internal.web.controller.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.currentUser
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementParticipantsAndGroupsModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageModel.Companion.paginate
import fr.laucoin.registry.backend.domain.service.IMovementService
import fr.laucoin.registry.backend.infrastructure.internal.web.controller.IMovementController
import fr.laucoin.registry.backend.infrastructure.internal.web.dto.MovementDto
import fr.laucoin.registry.backend.infrastructure.internal.web.mapper.MovementDtoMapper
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort.Direction
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class MovementController(
    private val service: IMovementService,
    private val mapper: MovementDtoMapper,
    @Value("\${registry.feature.movement.searched.max-participant-result}")
    private val maxParticipantResult: Int,
    @Value("\${registry.feature.movement.searched.max-group-result}")
    private val maxGroupResult: Int,
): IMovementController {
    override fun findMovements(
        eventId: UUID,
        offset: Int,
        limit: Int,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Mono<PageModel<MovementModel>> {
        return service.findMovements(eventId, order, onlyVisible, searched, type, startDateTime, endDateTime)
            .paginate(offset, limit)
    }

    override fun findMovementById(eventId: UUID, id: UUID): Mono<MovementModel> {
        return service.findMovementById(eventId, id, onlyVisible = false)
    }

    override fun searchParticipantsAndGroups(eventId: UUID, searched: String?): Mono<MovementParticipantsAndGroupsModel> {
        return service.searchParticipantsAndGroups(eventId, searched)
            .map {
                MovementParticipantsAndGroupsModel(
                    participants = it.participants.take(maxParticipantResult),
                    groups = it.groups.take(maxGroupResult)
                )
            }
    }

    override fun createMovement(eventId: UUID, movement: MovementDto): Mono<MovementModel> {
        return currentUser().flatMap { service.createMovement(it, mapper.toModel(movement, eventId)) }
    }

    override fun updateMovementById(eventId: UUID, id: UUID, movement: MovementDto): Mono<MovementModel> {
        return currentUser().flatMap { service.updateMovementById(it, eventId, id, mapper.toModel(movement, eventId)) }
    }

    override fun disableMovementById(eventId: UUID, id: UUID): Mono<MovementModel> {
        return currentUser().flatMap { service.disableMovementById(it, eventId, id) }
    }

    override fun enableMovementById(eventId: UUID, id: UUID): Mono<MovementModel> {
        return currentUser().flatMap { service.enableMovementById(it, eventId, id) }
    }

    override fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void> {
        return service.deleteMovementById(eventId, id)
    }
}
