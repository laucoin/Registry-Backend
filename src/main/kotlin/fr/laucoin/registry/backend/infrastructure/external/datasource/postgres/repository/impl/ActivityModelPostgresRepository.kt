package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.domain.model.ActivitySearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.IActivityModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IActivityEntityRepository
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ActivityModelPostgresRepository(
    private val repository: IActivityEntityRepository,
    private val mapper: ActivityEntityMapper,
): IActivityModelRepository {
    override fun findPage(
        eventId: UUID,
        pageable: PageableModel,
        searchParams: ActivitySearchParamModel,
    ): Mono<PageModel<ActivityModel>> {
        return Mono.zip(
            repository.countAll(
                eventId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.dateTimeSearched,
            ),
            repository.findAll(
                eventId,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.availabilitySearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList()
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findAllByIds(eventId: UUID, ids: List<UUID>, visibilitySearched: Boolean?): Flux<ActivityModel> {
        return if (ids.isEmpty()) Flux.empty() else repository.findAllByIds(eventId, ids, visibilitySearched).map(mapper::toModel)
    }

    override fun findWithLimit(limit: Int, eventId: UUID, searchParams: ActivitySearchParamModel): Flux<ActivityModel> {
        return repository.findWithLimit(
            eventId,
            searchParams.textSearched,
            searchParams.visibilitySearched,
            searchParams.availabilitySearched,
            searchParams.dateTimeSearched,
            limit,
        ).map(mapper::toModel)
    }

    override fun findById(eventId: UUID, id: UUID, visibilitySearched: Boolean?): Mono<ActivityModel> {
        return repository.findById(eventId, id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun create(element: ActivityModel): Mono<ActivityModel> {
        return save(element)
    }

    override fun update(element: ActivityModel): Mono<ActivityModel> {
        return save(element)
    }

    private fun save(element: ActivityModel): Mono<ActivityModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
