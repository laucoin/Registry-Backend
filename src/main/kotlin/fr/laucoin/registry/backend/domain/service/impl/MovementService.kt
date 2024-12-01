package fr.laucoin.registry.backend.domain.service.impl

import fr.laucoin.registry.backend.domain.constant.ErrorConst.MovementError.MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.extension.ReactiveExt.notFoundIfEmpty
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.domain.repository.IMovementContentModelRepository
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.domain.service.GenericService
import fr.laucoin.registry.backend.domain.service.IEventService
import fr.laucoin.registry.backend.domain.service.IMovementService
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.data.domain.Sort.Direction
import org.springframework.stereotype.Service
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class MovementService(
    private val repository: IMovementModelRepository,
    private val contentRepository: IMovementContentModelRepository,
    private val transactionalOperator: TransactionalOperator,
    private val eventService: IEventService,
): IMovementService, GenericService<MovementModel>(compareBy { it.dateTime }) {
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
            .searchAndSort(order, searched)
    }

    override fun findMovementById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<MovementModel> {
        return repository.findById(eventId, id, onlyVisible)
            .notFoundIfEmpty(id)
    }

    override fun createMovement(currentUser: UserModel, movement: MovementModel): Mono<MovementModel> {
        return eventService.validateDateTime(movement.event !!.id !!, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
            .flatMap { repository.save(movement.apply { create(currentUser) }) }
            .flatMap {
                val now = ZonedDateTime.now()
                contentRepository.saveAll(movement.content.map { content ->
                    content.apply {
                        movementId = it.id
                        create(currentUser, now)
                    }
                })
                    .collectList()
                    .map { newContent -> movement.apply { content = newContent } }
            }
            .`as`(transactionalOperator::transactional)
    }

    override fun updateMovementById(currentUser: UserModel, eventId: UUID, id: UUID, movement: MovementModel): Mono<MovementModel> {
        return eventService.validateDateTime(movement.event !!.id !!, movement.dateTime, MOVEMENT_DATETIME_OUT_OF_EVENT_DATE_RANGE)
            .flatMap { findMovementById(eventId, id, onlyVisible = false) }
            .flatMap {
                if (it.changed(movement)) {
                    it.apply {
                        dateTime = movement.dateTime
                        update(currentUser)
                    }
                    repository.save(it).thenReturn(it)
                } else Mono.just(it)
            }
            .flatMap {
                val newContent = it.getNewContent(movement).map { content -> content.apply { create(currentUser) } }
                if (newContent.isNotEmpty()) {
                    contentRepository.saveAll(newContent)
                        .collectList()
                        .map { savedContent -> it.apply { content.plus(savedContent) } }
                } else Mono.just(it)
            }
            .flatMap {
                val removedContent = it.getRemovedContent(movement).map { content ->
                    content.apply {
                        visible = false
                        update(currentUser)
                    }
                }
                contentRepository.saveAll(removedContent)
                    .collectList()
                    .map { content -> it.apply { content.minus(content.toSet()) } }
            }
            .`as`(transactionalOperator::transactional)
    }

    private fun Mono<MovementModel>.updateMovement(currentUser: UserModel) = flatMap {
        repository.save(it.apply { update(currentUser) })
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
