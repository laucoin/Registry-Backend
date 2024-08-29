package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.domain.repository.IEventModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class EventModelPostgresRepository(
    private val repository: IEventEntityRepository,
    private val mapper: EventEntityMapper,
): IEventModelRepository {
    override fun findAll(onlyVisible: Boolean, startDateTime: ZonedDateTime?, endDateTime: ZonedDateTime?): Flux<EventModel> {
        return repository.findAll(onlyVisible, startDateTime, endDateTime).map(mapper::toModel)
    }

    override fun validDateTime(id: UUID, begin: ZonedDateTime?, end: ZonedDateTime?): Mono<Boolean> {
        return repository.validDateTime(id, begin, end)
            .map { it.count == 0 }
    }

    override fun findById(id: UUID, onlyVisible: Boolean): Mono<EventModel> {
        return repository.findById(id, onlyVisible).map(mapper::toModel)
    }

    override fun save(element: EventModel): Mono<EventModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
