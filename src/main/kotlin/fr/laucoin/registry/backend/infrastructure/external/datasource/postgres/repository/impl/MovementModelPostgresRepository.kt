package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class MovementModelPostgresRepository(
    private val repository: IMovementEntityRepository,
    private val contentRepository: IMovementContentEntityRepository,
    private val transactionalOperator: TransactionalOperator,
    private val mapper: MovementEntityMapper,
    private val contentMapper: MovementContentEntityMapper,
): IMovementModelRepository {
    override fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<MovementModel> = repository.findAll(eventId, onlyVisible, type, startDateTime, endDateTime).map(mapper::toModel)

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<MovementModel> {
        return repository.findById(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun create(element: MovementModel): Mono<MovementModel> {
        return save(element)
            .saveNewContent(element)
            .`as`(transactionalOperator::transactional)
    }

    override fun update(element: MovementModel): Mono<MovementModel> {
        return save(element)
            .flatMap { findById(element.event?.id !!, element.id !!, onlyVisible = false) }
            .saveNewContent(element)
            .removeDeletedContent(element)
            .`as`(transactionalOperator::transactional)
    }

    @Transactional
    fun Mono<MovementModel>.saveNewContent(element: MovementModel): Mono<MovementModel> {
        return flatMap { movement ->
            val newContent = movement.getNewContent(element)
            if (newContent.isEmpty()) return@flatMap Mono.just(movement)
            contentRepository.saveAll(newContent.map { contentMapper.toEntity(movement.id !!, it) })
                .map(contentMapper::toModel)
                .collectList()
                .map { movement.apply { content = content.plus(it) } }
        }
    }

    @Transactional
    fun Mono<MovementModel>.removeDeletedContent(element: MovementModel): Mono<MovementModel> {
        return flatMap { movement ->
            val removedContent = movement.getRemovedContentParticipantIds(element)
            if (removedContent.isEmpty()) return@flatMap Mono.just(movement)
            contentRepository.deleteAllByMovementIdAndParticipantId(movement.id !!, removedContent)
                .then(Mono.fromCallable { movement.apply { content = content.filter { removedContent.contains(it.id) } } })
        }
    }

    private fun save(element: MovementModel): Mono<MovementModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
