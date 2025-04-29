package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.MovementSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.IMovementModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
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
    private val emptySearch = MovementSearchParamModel(
        visibilitySearched = null,
        typeSearched = null,
        startDateTimeSearched = null,
        endDateTimeSearched = null,
    )

    override fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel,
    ): Mono<PageModel<MovementModel>> {
        return Mono.zip(
            repository.countAll(
                projectId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
            ),
            repository.findAll(
                projectId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findContent(projectId: UUID, movementIds: List<UUID>): Flux<Pair<UUID, List<MovementContentModel>>> {
        return if (movementIds.isEmpty()) Flux.empty()
        else contentRepository.findAllByMovementIds(projectId, movementIds)
            .groupBy(MovementContentEntity::movementId)
            .flatMap {
                it.collectList().map { list -> it.key() to list.map(contentMapper::toModel) }
            }
    }

    override fun findPageByParticipantId(
        projectId: UUID,
        participantId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel
    ): Mono<PageModel<MovementModel>> {
        return Mono.zip(
            repository.countAllByParticipantId(
                projectId,
                participantId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
            ),
            repository.findAllByParticipantId(
                projectId,
                participantId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findPageByVehicleId(
        projectId: UUID,
        vehicleId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel
    ): Mono<PageModel<MovementModel>> {
        return Mono.zip(
            repository.countAllByVehicleId(
                projectId,
                vehicleId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
            ),
            repository.findAllByVehicleId(
                projectId,
                vehicleId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findPageByActivityId(
        projectId: UUID,
        activityId: UUID,
        pageable: PageableModel,
        searchParams: MovementSearchParamModel
    ): Mono<PageModel<MovementModel>> {
        return Mono.zip(
            repository.countAllByActivityId(
                projectId,
                activityId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
            ),
            repository.findAllByActivityId(
                projectId,
                activityId,
                searchParams.visibilitySearched,
                searchParams.typeSearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun countAllByParticipantId(projectId: UUID, participantId: UUID): Mono<Long> {
        return repository.countAllByParticipantId(
            projectId,
            participantId,
            emptySearch.visibilitySearched,
            emptySearch.typeSearched,
            emptySearch.startDateTimeSearched,
            emptySearch.endDateTimeSearched,
        )
    }

    override fun countAllByVehicleId(projectId: UUID, vehicleId: UUID): Mono<Long> {
        return repository.countAllByVehicleId(
            projectId,
            vehicleId,
            emptySearch.visibilitySearched,
            emptySearch.typeSearched,
            emptySearch.startDateTimeSearched,
            emptySearch.endDateTimeSearched,
        )
    }

    override fun countAllByActivityId(projectId: UUID, activityId: UUID): Mono<Long> {
        return repository.countAllByActivityId(
            projectId,
            activityId,
            emptySearch.visibilitySearched,
            emptySearch.typeSearched,
            emptySearch.startDateTimeSearched,
            emptySearch.endDateTimeSearched,
        )
    }

    override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<MovementModel> {
        return Mono.zip(
            repository.findById(projectId, id, visibilitySearched).map(mapper::toModel),
            findContent(projectId, listOf(id)).collectList()
                .handle { it, handle -> if (it.isNullOrEmpty()) handle.next(emptyList()) else handle.next(it.first().second) }
        ).map {
            it.t1.content = it.t2
            it.t1
        }
    }

    override fun create(element: MovementModel): Mono<MovementModel> {
        return save(element)
            .saveNewContent(element)
            .`as`(transactionalOperator::transactional)
    }

    override fun update(element: MovementModel): Mono<MovementModel> {
        return save(element)
            .flatMap { findById(element.project !!.id !!, element.id !!, visibilitySearched = null) }
            .removeDeletedContent(element)
            .saveNewContent(element)
            .`as`(transactionalOperator::transactional)
    }

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

    fun Mono<MovementModel>.removeDeletedContent(element: MovementModel): Mono<MovementModel> {
        return flatMap { movement ->
            val removedIds = movement.getOldContentIds(element)
            if (removedIds.isEmpty()) return@flatMap Mono.just(movement)
            contentRepository.deleteAllById(removedIds)
                .then(Mono.fromCallable { movement.apply { content = content.filter { ! removedIds.contains(it.id) } } })
        }
    }

    private fun save(element: MovementModel): Mono<MovementModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
