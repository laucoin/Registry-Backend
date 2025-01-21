package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_EVENT
import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_PARTICIPANTS_NOT_VISIBLE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementParticipantsAndGroupsModel
import fr.laucoin.registry.backend.domain.model.RegistryException
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IGroupModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.repository.IParticipantModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IMovementService
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Mono.zip

@Service
class MovementService(
    private val repository: IMovementModelRepository,
    private val eventService: IEventService,
    private val participantRepository: IParticipantModelRepository,
    private val groupRepository: IGroupModelRepository,
): IMovementService, GenericService() {
    override fun findMovements(
        eventId: UUID,
        order: Direction,
        onlyVisible: Boolean,
        searched: String?,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementModel> {
        return repository.findAll(eventId, onlyVisible, type, startDateTime, endDateTime)
            .searchAndSort(order, searched, compareBy { it.dateTime })
    }

    override fun findMovementById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<MovementModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun searchParticipantsAndGroups(eventId: UUID, searched: String?): Mono<MovementParticipantsAndGroupsModel> {
        return zip(
            participantRepository.findAll(
                eventId,
                onlyVisible = true,
                onlyPresent = false,
                startDateTime = null,
                endDateTime = null
            ).searchAndSort(ASC, searched, compareBy { it.lastName })
                .collectList(),
            groupRepository.findAll(
                eventId,
                onlyVisible = true,
                onlyPresent = true,
                startDateTime = null,
                endDateTime = null
            ).searchAndSort(ASC, searched, compareBy { it.name })
                .collectList(),
        ).map { MovementParticipantsAndGroupsModel(participants = it.t1, groups = it.t2) }
    }

    override fun createMovement(currentUser: UserModel, movement: MovementModel): Mono<MovementModel> {
        return eventService.validateDateTime(movement.event !!.id !!, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
            .flatMap {
                validateParticipants(
                    movement.event !!.id !!,
                    movement,
                    movement.content.mapNotNull { c -> c.participant?.id })
            }
            .flatMap { repository.create(movement.apply { create(currentUser) }) }
    }

    override fun updateMovementById(currentUser: UserModel, eventId: UUID, id: UUID, movement: MovementModel): Mono<MovementModel> {
        return eventService.validateDateTime(movement.event !!.id !!, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
            .flatMap { findMovementById(eventId, id, onlyVisible = false) }
            .flatMap {
                val newParticipantIds: List<UUID> = it.getNewContentParticipantIds(movement)
                if (newParticipantIds.isEmpty()) Mono.just(it)
                else validateParticipants(eventId, it, newParticipantIds)
            }
            .flatMap {
                it.let {
                    it.dateTime = movement.dateTime
                    it.content = movement.content
                    it.update(currentUser)
                }
                repository.update(it)
            }
    }

    private fun Mono<MovementModel>.updateMovement(currentUser: UserModel) = flatMap {
        repository.update(it.apply { update(currentUser) })
    }

    private fun validateParticipants(eventId: UUID, movement: MovementModel, newParticipantIds: List<UUID>): Mono<MovementModel> {
        return participantRepository.findAllByIds(eventId, newParticipantIds, onlyVisible = false)
            .collectList()
            .handle { it, handle ->
                when {
                    it.size != newParticipantIds.size -> handle.error(
                        RegistryException(
                            NOT_FOUND,
                            MOVEMENT_PARTICIPANTS_NOT_FOUND_IN_MOVEMENT_EVENT,
                        )
                    )

                    it.any { m -> m.isNotVisible() || m.purged == true } -> handle.error(
                        RegistryException(
                            CONFLICT,
                            MOVEMENT_PARTICIPANTS_NOT_VISIBLE,
                        )
                    )

                    else -> handle.next(movement)
                }
            }
    }

    override fun disableMovementById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<MovementModel> {
        return findMovementById(eventId, id, onlyVisible = true)
            .updateVisibility(visibility = false)
            .updateMovement(currentUser)
    }

    override fun enableMovementById(currentUser: UserModel, eventId: UUID, id: UUID): Mono<MovementModel> {
        return findMovementById(eventId, id, onlyVisible = false)
            .updateVisibility(visibility = true)
            .updateMovement(currentUser)
    }

    override fun deleteMovementById(eventId: UUID, id: UUID): Mono<Void> {
        return findMovementById(eventId, id, onlyVisible = false)
            .flatMap { repository.deleteById(id) }
    }
}
