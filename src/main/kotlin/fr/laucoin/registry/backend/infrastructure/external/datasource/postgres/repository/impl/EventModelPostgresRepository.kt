package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.model.EventSearchParamModel
import fr.laucoin.registry.backend.domain.model.PageModel
import fr.laucoin.registry.backend.domain.model.PageableModel
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventEntityRepository
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class EventModelPostgresRepository(
    private val repository: IEventEntityRepository,
    private val mapper: EventEntityMapper,
): IEventModelRepository {
    override fun findPage(
        pageable: PageableModel,
        searchParams: EventSearchParamModel,
    ): Mono<PageModel<EventModel>> {
        return Mono.zip(
            repository.countAll(
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.dateTimeSearched,
            ),
            repository.findAll(
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList(),
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun findPage(
        eventIds: List<UUID>,
        pageable: PageableModel,
        searchParams: EventSearchParamModel
    ): Mono<PageModel<EventModel>> {
        if (eventIds.isEmpty()) {
            return Mono.just(PageModel(pageable, 0, emptyList()))
        }

        return Mono.zip(
            repository.countAllInEventIds(
                eventIds,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.dateTimeSearched,
            ),
            repository.findAllInEventIds(
                eventIds,
                searchParams.textSearched,
                searchParams.visibilitySearched,
                searchParams.dateTimeSearched,
                pageable.limit,
                pageable.offset,
            ).map(mapper::toModel).collectList(),
        ).map {
            PageModel(pageable, it.t1, it.t2)
        }
    }

    override fun validDateTime(id: UUID, begin: LocalDateTime?, end: LocalDateTime?): Mono<Boolean> {
        return repository.validDateTime(id, begin, end)
            .map { (it.count ?: 0) == 0 }
    }

    override fun findById(id: UUID, visibilitySearched: Boolean?): Mono<EventModel> {
        return repository.findById(id, visibilitySearched)
            .map(mapper::toModel)
            .switchIfEmpty(Mono.empty())
    }

    override fun create(element: EventModel): Mono<EventModel> {
        return save(element)
    }

    override fun update(element: EventModel): Mono<EventModel> {
        return save(element)
    }

    private fun save(element: EventModel): Mono<EventModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
