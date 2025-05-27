package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.CommunicationModel
import fr.laucoin.registry.backend.domain.model.CommunicationSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.ICommunicationModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.communication.CommunicationEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.CommunicationEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.ICommunicationEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class CommunicationPostgresRepository(
    private val repository: ICommunicationEntityRepository,
    private val mapper: CommunicationEntityMapper,
): ICommunicationModelRepository {
    override fun findPage(
        projectId: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel,
    ): Mono<PageModel<CommunicationModel>> {
        return Mono.zip(
            repository.countAll(
                projectId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
            ),
            repository.findAll(
                projectId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findByMovementIdsWithLimit(
        limit: Int,
        projectId: UUID,
        movementIds: List<UUID>,
        visibilitySearched: Boolean?,
    ): Flux<Pair<UUID, List<CommunicationModel>>> {
        return if (movementIds.isEmpty()) Flux.empty()
        else repository.findAllByMovementIdsWithLimit(projectId, movementIds, visibilitySearched, limit)
            .groupBy(CommunicationEntity::movementId)
            .flatMap {
                it.collectList().map { list -> it.key() to list.map(mapper::toModel) }
            }
    }

    override fun findPageByMovementId(
        projectId: UUID,
        movementId: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel
    ): Mono<PageModel<CommunicationModel>> {
        return Mono.zip(
            countAllByMovementId(projectId, movementId, searchParams),
            repository.findAllByMovementId(
                projectId,
                movementId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findByAlertIdsWithLimit(
        limit: Int,
        projectId: UUID,
        alertIds: List<UUID>,
        visibilitySearched: Boolean?
    ): Flux<Pair<UUID, List<CommunicationModel>>> {
        return if (alertIds.isEmpty()) Flux.empty()
        else repository.findAllByAlertIdsWithLimit(projectId, alertIds, visibilitySearched, limit)
            .groupBy(CommunicationEntity::movementId)
            .flatMap {
                it.collectList().map { list -> it.key() to list.map(mapper::toModel) }
            }
    }

    override fun findPageByAlertId(
        projectId: UUID,
        alertId: UUID,
        pageable: PageableModel,
        searchParams: CommunicationSearchParamModel
    ): Mono<PageModel<CommunicationModel>> {
        return Mono.zip(
            countAllByAlertId(projectId, alertId, searchParams),
            repository.findAllByAlertId(
                projectId,
                alertId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.startDateTimeSearched,
                searchParams.endDateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findAllByIds(
        projectId: UUID,
        ids: List<UUID>,
        visibilitySearched: Boolean?
    ): Flux<CommunicationModel> {
        return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(projectId, ids, visibilitySearched).map(mapper::toModel)
    }

    override fun countAllByMovementId(
        projectId: UUID,
        movementId: UUID,
        searchParams: CommunicationSearchParamModel
    ): Mono<Long> {
        return repository.countAllByMovementId(
            projectId,
            movementId,
            searchParams.textSearched,
            searchParams.visibilitySearched,
            searchParams.startDateTimeSearched,
            searchParams.endDateTimeSearched,
        )
    }

    override fun countAllByAlertId(
        projectId: UUID,
        alertId: UUID,
        searchParams: CommunicationSearchParamModel
    ): Mono<Long> {
        return repository.countAllByAlertId(
            projectId,
            alertId,
            searchParams.textSearched,
            searchParams.visibilitySearched,
            searchParams.startDateTimeSearched,
            searchParams.endDateTimeSearched,
        )
    }

    override fun findById(projectId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<CommunicationModel> {
        return repository.findById(projectId, id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun create(element: CommunicationModel): Mono<CommunicationModel> {
        return save(element)
    }

    override fun update(element: CommunicationModel): Mono<CommunicationModel> {
        return save(element)
    }

    private fun save(element: CommunicationModel): Mono<CommunicationModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
